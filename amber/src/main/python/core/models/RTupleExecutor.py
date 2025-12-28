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

import datetime
import pickle
import pyarrow as pa
import rpy2
import rpy2.robjects as robjects
import warnings
from rpy2.robjects import default_converter
from rpy2.robjects.conversion import localconverter as local_converter
from rpy2_arrow.arrow import converter as arrow_converter
from typing import Iterator, Optional, Union

from core.models import Tuple, TupleLike, TableLike, r_utils
from core.models.operator import SourceOperator, TupleOperatorV2
from core.models.schema.attribute_type_utils import (
    TEXERA_TYPE_METADATA_KEY,
    LARGE_BINARY_METADATA_VALUE,
)
from core.models.type.large_binary import largebinary

warnings.filterwarnings(action="ignore", category=UserWarning, module=r"rpy2*")


class RTupleExecutor(TupleOperatorV2):
    """
    An operator that can execute R code on R Lists (R's representation of a Tuple)
    """

    is_source = False

    _combine_binary_and_non_binary_lists = robjects.r(
        """
        function(non_binary_list, binary_list) {
            non_binary_list <- as.list(non_binary_list$as_vector())
            return (c(non_binary_list, binary_list))
        }
        """
    )

    @staticmethod
    def _setup_r_large_binary_apis():
        """
        Set up R functions for large binary operations.
        These are pure R implementations that use aws.s3 to interact with S3,
        avoiding the garbage collection issues with Python object wrappers.
        """
        # Pass S3 configuration from Python to R via environment variables
        # so R code can access it
        import os
        from core.storage.storage_config import StorageConfig

        # Set environment variables that R can read
        # Use getattr to handle cases where StorageConfig might not be initialized
        s3_endpoint = getattr(StorageConfig, "S3_ENDPOINT", None) or os.getenv(
            "STORAGE_S3_ENDPOINT", "http://localhost:9000"
        )
        s3_region = getattr(StorageConfig, "S3_REGION", None) or os.getenv(
            "STORAGE_S3_REGION", "us-west-2"
        )
        s3_username = getattr(StorageConfig, "S3_AUTH_USERNAME", None) or os.getenv(
            "STORAGE_S3_AUTH_USERNAME", "texera_minio"
        )
        s3_password = getattr(StorageConfig, "S3_AUTH_PASSWORD", None) or os.getenv(
            "STORAGE_S3_AUTH_PASSWORD", "password"
        )

        os.environ["STORAGE_S3_ENDPOINT"] = s3_endpoint
        os.environ["STORAGE_S3_REGION"] = s3_region
        os.environ["STORAGE_S3_AUTH_USERNAME"] = s3_username
        os.environ["STORAGE_S3_AUTH_PASSWORD"] = s3_password

        # Inject pure R implementations into R environment
        robjects.r("""
        # Load aws.s3 package (required for S3 operations)
        if (!requireNamespace("aws.s3", quietly = TRUE)) {
            stop("Package 'aws.s3' is required for LargeBinary operations. Please install it with: install.packages('aws.s3')")
        }
        library(aws.s3)

        DEFAULT_BUCKET <- "texera-large-binaries"
        
        # Setup S3 configuration
        setup_s3_config <- function() {
            s3_endpoint <- Sys.getenv("STORAGE_S3_ENDPOINT", "http://localhost:9000")
            s3_region <- Sys.getenv("STORAGE_S3_REGION", "us-west-2")
            s3_username <- Sys.getenv("STORAGE_S3_AUTH_USERNAME", "texera_minio")
            s3_password <- Sys.getenv("STORAGE_S3_AUTH_PASSWORD", "password")
            
            Sys.setenv(
                AWS_ACCESS_KEY_ID = s3_username,
                AWS_SECRET_ACCESS_KEY = s3_password,
                AWS_S3_ENDPOINT = s3_endpoint,
                AWS_DEFAULT_REGION = s3_region
            )
            
            list(
                endpoint = s3_endpoint,
                region = s3_region,
                username = s3_username,
                password = s3_password,
                base_url = sub("^https?://", "", s3_endpoint),
                use_https = grepl("^https://", s3_endpoint)
            )
        }
        
        # Parse S3 URI into bucket and object key
        parse_s3_uri <- function(uri) {
            if (!grepl("^s3://", uri)) {
                stop("Invalid S3 URI: ", uri)
            }
            path_without_scheme <- sub("^s3://", "", uri)
            parts <- strsplit(path_without_scheme, "/", fixed = TRUE)[[1]]
            if (length(parts) < 2) {
                stop("Invalid S3 URI format: ", uri)
            }
            list(bucket = parts[1], object_key = paste(parts[-1], collapse = "/"))
        }
        
        # Generate unique ID
        generate_uuid <- function() {
            paste(sample(c(0:9, letters[1:6]), 32, replace = TRUE), collapse = "")
        }

        # Create or wrap a largebinary URI
        largebinary <- function(uri = NULL) {
            if (is.null(uri)) {
                config <- setup_s3_config()
                
                # Ensure bucket exists
                tryCatch({
                    head_bucket(DEFAULT_BUCKET, base_url = config$base_url, use_https = config$use_https,
                               region = config$region, key = config$username, secret = config$password)
                }, error = function(e) {
                    tryCatch({
                        put_bucket(DEFAULT_BUCKET, base_url = config$base_url, use_https = config$use_https,
                                  region = config$region, key = config$username, secret = config$password)
                    }, error = function(e2) {
                        # Ignore errors if bucket already exists
                    })
                })
                
                # Generate unique URI with timestamp (milliseconds since epoch)
                # Use numeric (not integer) since milliseconds exceed R integer max value
                timestamp_ms <- as.numeric(Sys.time()) * 1000
                if (is.na(timestamp_ms) || !is.finite(timestamp_ms)) {
                    timestamp_ms <- as.numeric(Sys.time()) * 1000
                }
                # Format without scientific notation
                timestamp_ms_str <- sprintf("%.0f", timestamp_ms)
                unique_id <- generate_uuid()
                uri <- paste0("s3://", DEFAULT_BUCKET, "/objects/", timestamp_ms_str, "/", unique_id)
            } else if (!is.character(uri) || length(uri) != 1) {
                stop("uri must be a single character string")
            } else if (!grepl("^s3://", uri)) {
                stop("largebinary URI must start with 's3://', got: ", uri)
            }
            return(uri)
        }

        # Stream for reading from S3
        LargeBinaryInputStream <- function(uri) {
            if (!is.character(uri)) stop("Expected character URI string")
            
            parsed <- parse_s3_uri(uri)
            config <- setup_s3_config()
            
            # Create streaming connection (downloads on-demand)
            tryCatch({
                s3connection(
                    object = parsed$object_key,
                    bucket = parsed$bucket,
                    region = config$region,
                    base_url = config$base_url,
                    use_https = config$use_https,
                    key = config$username,
                    secret = config$password
                )
            }, error = function(e) {
                stop("Failed to open streaming connection for ", uri, ": ", conditionMessage(e))
            })
        }

        # Stream for writing to S3
        LargeBinaryOutputStream <- function(uri) {
            if (!is.character(uri)) stop("Expected character URI string")
            
            parsed <- parse_s3_uri(uri)
            config <- setup_s3_config()
            
            temp_file <- tempfile()
            file_conn <- file(temp_file, open = "wb")
            closed <- FALSE
            
            write_func <- function(data) {
                if (closed) stop("I/O operation on closed stream")
                writeBin(data, file_conn)
            }
            
            # Uploads to S3 and cleans up
            close_func <- function() {
                if (!closed) {
                    closed <<- TRUE
                    close(file_conn)
                    
                    tryCatch({
                        put_object(
                            file = temp_file,
                            object = parsed$object_key,
                            bucket = parsed$bucket,
                            base_url = config$base_url,
                            use_https = config$use_https,
                            region = config$region,
                            key = config$username,
                            secret = config$password
                        )
                    }, error = function(e) {
                        unlink(temp_file)
                        stop("Failed to upload to S3: ", conditionMessage(e))
                    })
                    unlink(temp_file)
                }
            }
            
            stream_obj <- list(write = write_func, close = close_func)
            class(stream_obj) <- "LargeBinaryOutputStream"
            return(stream_obj)
        }
        """)

    def __init__(self, r_code: str):
        """
        Initialize the RTupleExecutor with R code.

        Args:
            r_code (str): R code to be executed.
        """
        super().__init__()
        # Set up R large binary APIs before loading user code
        RTupleExecutor._setup_r_large_binary_apis()
        # Use the local converter from rpy2 to load in the R function given by the user
        with local_converter(default_converter):
            self._func = robjects.r(r_code)

    def process_tuple(self, tuple_: Tuple, port: int) -> Iterator[Optional[TupleLike]]:
        """
        Process an input Tuple from the given link.

        :param tuple_: Tuple, a Tuple from an input port to be processed.
        :param port: int, input port index of the current Tuple.
        :return: Iterator[Optional[TupleLike]], producing one TupleLike object at a
            time, or None.
        """
        with local_converter(arrow_converter):
            input_schema: pa.Schema = tuple_._schema.as_arrow_schema()
            input_fields: list[str] = [field.name for field in input_schema]

            # Separate fields by type: regular, binary, and large_binary
            non_binary_fields: list[str] = []
            binary_fields: list[str] = []
            large_binary_fields: list[str] = []

            for field in input_schema:
                # Check if it's LARGE_BINARY by metadata
                is_large_binary = (
                    field.metadata
                    and field.metadata.get(TEXERA_TYPE_METADATA_KEY)
                    == LARGE_BINARY_METADATA_VALUE
                )
                if is_large_binary:
                    large_binary_fields.append(field.name)
                elif field.type == pa.binary():
                    binary_fields.append(field.name)
                else:
                    non_binary_fields.append(field.name)

            non_binary_pyarrow_array: pa.StructArray = pa.array([], type=pa.struct([]))
            if non_binary_fields:
                non_binary_tuple: Tuple = tuple_.get_partial_tuple(non_binary_fields)
                non_binary_tuple_schema: pa.Schema = (
                    non_binary_tuple._schema.as_arrow_schema()
                )
                non_binary_pyarrow_array: pa.StructArray = pa.array(
                    [non_binary_tuple.as_dict()],
                    type=pa.struct(non_binary_tuple_schema),
                )

            binary_r_list: dict[str, object] = {}
            if binary_fields:
                binary_tuple: Tuple = tuple_.get_partial_tuple(binary_fields)
                for k, v in binary_tuple.as_dict().items():
                    if isinstance(v, bytes):
                        binary_r_list[k] = pickle.loads(v[10:])
                    elif isinstance(v, datetime.datetime):
                        binary_r_list[k] = robjects.vectors.POSIXct.sexp_from_datetime(
                            [v]
                        )
                    else:
                        binary_r_list[k] = v

            # Handle LARGE_BINARY fields: convert to URI strings for R
            # R code can use largebinary(uri_string) to create largebinary objects if needed
            large_binary_r_list: dict[str, object] = {}
            if large_binary_fields:
                large_binary_tuple: Tuple = tuple_.get_partial_tuple(
                    large_binary_fields
                )
                for k, v in large_binary_tuple.as_dict().items():
                    if isinstance(v, largebinary):
                        # Convert largebinary object to URI string for R
                        large_binary_r_list[k] = v.uri
                    elif isinstance(v, str):
                        # Already a URI string, pass as-is
                        large_binary_r_list[k] = v
                    else:
                        large_binary_r_list[k] = v

            # Combine binary and large_binary into one list
            all_binary_r_list: dict[str, object] = {
                **binary_r_list,
                **large_binary_r_list,
            }
            binary_r_list: rpy2.robjects.ListVector = robjects.vectors.ListVector(
                all_binary_r_list
            )

            input_r_list: rpy2.robjects.ListVector = (
                RTupleExecutor._combine_binary_and_non_binary_lists(
                    non_binary_pyarrow_array, binary_r_list
                )
            )

            output_r_generator: rpy2.robjects.SignatureTranslatedFunction = self._func(
                input_r_list, port
            )

            while True:
                output_py_tuple: Tuple = r_utils.extract_tuple_from_r(
                    output_r_generator, False, input_fields, large_binary_fields
                )
                yield output_py_tuple if output_py_tuple is not None else None
                if output_py_tuple is None:
                    break


class RTupleSourceExecutor(SourceOperator):
    """
    A source operator that produces a generator that yields R Lists using R code.
    R Lists are R's representation of a Tuple
    """

    is_source = True

    def __init__(self, r_code: str):
        """
        Initialize the RTupleSourceExecutor with R code.

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
        Produce Tuples using the provided R generator returned by the UDF.
        The returned R generator is an iterator
        that yields R Lists (R's representation of Tuple)
        Used by the source operator only.

        :return: Iterator[Union[TupleLike, TableLike, None]], producing
            one TupleLike object, one TableLike object, or None, at a time.
        """
        with local_converter(arrow_converter):
            output_r_generator: rpy2.robjects.SignatureTranslatedFunction = self._func()
            while True:
                output_py_tuple: Tuple = r_utils.extract_tuple_from_r(
                    output_r_generator, True
                )
                yield output_py_tuple if output_py_tuple is not None else None
                if output_py_tuple is None:
                    break
