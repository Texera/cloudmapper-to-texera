# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

import pyarrow as pa
import rpy2.robjects as robjects
import typing
from rpy2.robjects import default_converter
from rpy2.robjects.conversion import localconverter as local_converter
from rpy2_arrow.arrow import rarrow_to_py_table, converter as arrow_converter
from typing import Iterator, Optional, Union

from core.models import ArrowTableTupleProvider, Tuple, TupleLike, Table, TableLike
from core.models.operator import SourceOperator, TableOperator
from core.models.schema.attribute_type_utils import (
    TEXERA_TYPE_METADATA_KEY,
    LARGE_BINARY_METADATA_VALUE,
)
from core.models.type.large_binary import largebinary
from core.models.RTupleExecutor import RTupleExecutor


class RTableExecutor(TableOperator):
    """
    An executor that can execute R code on Arrow tables.
    """

    is_source = False

    _arrow_to_r_dataframe = robjects.r(
        "function(table) { return (as.data.frame(table)) }"
    )

    _r_dataframe_to_arrow = robjects.r(
        """
        library(arrow)
        function(df) { return (arrow::as_arrow_table(df)) }
        """
    )

    def __init__(self, r_code: str):
        """
        Initialize the RTableExecutor with R code.

        Args:
            r_code (str): R code to be executed.
        """
        super().__init__()
        # Set up R large binary APIs before loading user code
        RTupleExecutor._setup_r_large_binary_apis()
        with local_converter(default_converter):
            self._func: typing.Callable[[pa.Table], pa.Table] = robjects.r(r_code)

    @staticmethod
    def _convert_largebinary_to_string_in_dataframe(df):
        """
        Convert largebinary objects to URI strings in a pandas DataFrame.
        Also detects columns containing S3 URI strings that should be LARGE_BINARY.
        This is needed because PyArrow cannot directly convert largebinary objects.

        Args:
            df: pandas DataFrame that may contain largebinary objects or URI strings

        Returns:
            tuple: (pandas DataFrame with largebinary objects converted to URI strings,
                   set of column names that contained largebinary objects or URI strings)
        """
        df = df.copy()
        large_binary_columns = set()
        for col in df.columns:
            # Check if column contains largebinary objects
            if len(df[col]) > 0:
                first_val = df[col].iloc[0]
                if isinstance(first_val, largebinary):
                    large_binary_columns.add(col)
                    # Convert all largebinary objects in this column to URI strings
                    df[col] = df[col].apply(
                        lambda x: x.uri if isinstance(x, largebinary) else x
                    )
                elif isinstance(first_val, str) and first_val.startswith("s3://"):
                    # Detect S3 URI strings that should be treated as LARGE_BINARY
                    # Check if all non-null values in the column are S3 URIs
                    non_null_values = df[col].dropna()
                    if len(non_null_values) > 0:
                        # Check if at least 80% of non-null values are S3 URIs
                        s3_uri_count = sum(
                            1
                            for v in non_null_values
                            if isinstance(v, str) and v.startswith("s3://")
                        )
                        if (
                            s3_uri_count > 0
                            and (s3_uri_count / len(non_null_values)) >= 0.8
                        ):
                            large_binary_columns.add(col)
        return df, large_binary_columns

    @staticmethod
    def _convert_largebinary_to_string_in_table(table: pa.Table) -> pa.Table:
        """
        Convert largebinary objects to URI strings in the Arrow table.
        This ensures LARGE_BINARY fields are properly serialized as strings.
        Arrow conversion should handle this automatically, but this is a safety check.
        """
        # Arrow tables from R should already have largebinary objects converted to strings
        # by the Arrow conversion process. This function is kept for potential future use.
        return table

    @staticmethod
    def _add_large_binary_metadata_to_schema(
        table: pa.Table, large_binary_columns: set
    ) -> pa.Table:
        """
        Add LARGE_BINARY metadata to fields in the output table.

        Args:
            table: Output Arrow table
            large_binary_columns: Set of column names that should have LARGE_BINARY metadata

        Returns:
            Arrow table with LARGE_BINARY metadata added
        """
        if not large_binary_columns:
            return table

        # Check if we need to add metadata
        needs_rebuild = False
        new_fields = []
        for field in table.schema:
            if field.name in large_binary_columns and field.type == pa.string():
                # Check if metadata is already present
                has_metadata = (
                    field.metadata
                    and field.metadata.get(TEXERA_TYPE_METADATA_KEY)
                    == LARGE_BINARY_METADATA_VALUE
                )
                if not has_metadata:
                    # This field should have LARGE_BINARY metadata
                    new_field = pa.field(
                        field.name,
                        field.type,
                        metadata={
                            TEXERA_TYPE_METADATA_KEY: LARGE_BINARY_METADATA_VALUE
                        },
                    )
                    needs_rebuild = True
                else:
                    new_field = field
            else:
                new_field = field
            new_fields.append(new_field)

        if needs_rebuild:
            # Rebuild the table with the new schema
            new_schema = pa.schema(new_fields)
            return pa.Table.from_arrays(
                [table.column(i) for i in range(table.num_columns)], schema=new_schema
            )
        return table

    def process_table(self, table: Table, port: int) -> Iterator[Optional[TableLike]]:
        """
        Process an input Table using the provided R function.
        The Table is represented as a pandas.DataFrame.

        :param table: Table, a table to be processed.
        :param port: int, input port index of the current Tuple.
            Currently unused in R-UDF
        :return: Iterator[Optional[TableLike]], producing one TableLike object at a
        time, or None.
        """
        # Convert any largebinary objects to URI strings before Arrow conversion
        # Track which columns contained largebinary objects
        table, input_large_binary_columns = (
            RTableExecutor._convert_largebinary_to_string_in_dataframe(table)
        )
        input_pyarrow_table = pa.Table.from_pandas(table)

        # Add LARGE_BINARY metadata to input table schema
        input_pyarrow_table = RTableExecutor._add_large_binary_metadata_to_schema(
            input_pyarrow_table, input_large_binary_columns
        )

        with local_converter(arrow_converter):
            input_r_dataframe = RTableExecutor._arrow_to_r_dataframe(
                input_pyarrow_table
            )
            output_r_dataframe = self._func(input_r_dataframe, port)
            output_rarrow_table = RTableExecutor._r_dataframe_to_arrow(
                output_r_dataframe
            )
            output_pyarrow_table = rarrow_to_py_table(output_rarrow_table)

            # Convert Arrow table to pandas, convert any largebinary objects to strings, then back to Arrow
            output_pandas_df = output_pyarrow_table.to_pandas()
            output_pandas_df, output_large_binary_columns = (
                RTableExecutor._convert_largebinary_to_string_in_dataframe(
                    output_pandas_df
                )
            )
            output_pyarrow_table = pa.Table.from_pandas(output_pandas_df)

            # Combine input and output large_binary columns (output might have new ones)
            all_large_binary_columns = (
                input_large_binary_columns | output_large_binary_columns
            )

            # Add LARGE_BINARY metadata to output table schema
            output_pyarrow_table = RTableExecutor._add_large_binary_metadata_to_schema(
                output_pyarrow_table, all_large_binary_columns
            )

        # Create a Schema from the Arrow schema to pass to Tuple
        from core.models.schema import Schema

        output_schema = Schema(arrow_schema=output_pyarrow_table.schema)

        for field_accessor in ArrowTableTupleProvider(output_pyarrow_table):
            yield Tuple(
                {name: field_accessor for name in output_pyarrow_table.column_names},
                schema=output_schema,
            )


class RTableSourceExecutor(SourceOperator):
    """
    A source operator that produces an R Table or Table-like object using R code.
    """

    is_source = True
    _source_output_to_arrow = robjects.r(
        """
    library(arrow)
    function(source_output) {
        return (arrow::as_arrow_table(as.data.frame(source_output)))
    }
    """
    )

    def __init__(self, r_code: str):
        """
        Initialize the RTableSourceExecutor with R code.

        Args:
            r_code (str): R code to be executed.
        """
        super().__init__()
        # Set up R large binary APIs before loading user code
        RTupleExecutor._setup_r_large_binary_apis()
        # Use the local converter from rpy2 to load in the R function given by the user
        with local_converter(default_converter):
            self._func = robjects.r(r_code)

    def produce(self) -> Iterator[Union[TupleLike, TableLike, None]]:
        """
        Produce Table using the provided R function.
        Used by the source operator only.

        :return: Iterator[Union[TupleLike, TableLike, None]], producing
            one TupleLike object, one TableLike object, or None, at a time.
        """
        with local_converter(arrow_converter):
            output_table = self._func()
            output_rarrow_table = RTableSourceExecutor._source_output_to_arrow(
                output_table
            )
            output_pyarrow_table = rarrow_to_py_table(output_rarrow_table)

            # Convert Arrow table to pandas, convert any largebinary objects to strings, then back to Arrow
            output_pandas_df = output_pyarrow_table.to_pandas()
            output_pandas_df, output_large_binary_columns = (
                RTableExecutor._convert_largebinary_to_string_in_dataframe(
                    output_pandas_df
                )
            )
            output_pyarrow_table = pa.Table.from_pandas(output_pandas_df)

            # Add LARGE_BINARY metadata to output table schema
            output_pyarrow_table = RTableExecutor._add_large_binary_metadata_to_schema(
                output_pyarrow_table, output_large_binary_columns
            )

        # Create a Schema from the Arrow schema to pass to Tuple
        from core.models.schema import Schema

        output_schema = Schema(arrow_schema=output_pyarrow_table.schema)

        for field_accessor in ArrowTableTupleProvider(output_pyarrow_table):
            yield Tuple(
                {name: field_accessor for name in output_pyarrow_table.column_names},
                schema=output_schema,
            )
