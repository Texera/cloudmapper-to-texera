package edu.uci.ics.amber.operator.udf.python.source;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.google.common.base.Preconditions;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaTitle;
import edu.uci.ics.amber.core.executor.OpExecInitInfo;
import edu.uci.ics.amber.core.tuple.Attribute;
import edu.uci.ics.amber.core.tuple.Schema;
import edu.uci.ics.amber.core.workflow.PhysicalOp;
import edu.uci.ics.amber.core.workflow.SchemaPropagationFunc;
import edu.uci.ics.amber.operator.metadata.OperatorGroupConstants;
import edu.uci.ics.amber.operator.metadata.OperatorInfo;
import edu.uci.ics.amber.operator.source.SourceOperatorDescriptor;
import edu.uci.ics.amber.operator.util.OperatorDescriptorUtils;
import edu.uci.ics.amber.virtualidentity.ExecutionIdentity;
import edu.uci.ics.amber.virtualidentity.WorkflowIdentity;
import edu.uci.ics.amber.workflow.InputPort;
import edu.uci.ics.amber.workflow.OutputPort;
import edu.uci.ics.amber.workflow.PortIdentity;
import scala.Option;
import scala.collection.immutable.Map;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.util.Collections.singletonList;
import static scala.jdk.javaapi.CollectionConverters.asScala;


public class PythonUDFSourceOpDescV2 extends SourceOperatorDescriptor {

    @JsonProperty(required = true, defaultValue =
            "# Choose from the following templates:\n" +
                    "# \n" +
                    "# from pytexera import *\n" +
                    "# \n" +
                    "# class GenerateOperator(UDFSourceOperator):\n" +
                    "# \n" +
                    "#     @overrides\n" +
                    "#     def produce(self) -> Iterator[Union[TupleLike, TableLike, None]]:\n" +
                    "#         yield\n")
    @JsonSchemaTitle("Python script")
    @JsonPropertyDescription("Input your code here")
    public String code;

    @JsonProperty(required = true, defaultValue = "1")
    @JsonSchemaTitle("Worker count")
    @JsonPropertyDescription("Specify how many parallel workers to lunch")
    public Integer workers = 1;

    @JsonProperty()
    @JsonSchemaTitle("Columns")
    @JsonPropertyDescription("The columns of the source")
    public List<Attribute> columns;

    @Override
    public PhysicalOp getPhysicalOp(WorkflowIdentity workflowId, ExecutionIdentity executionId) {
        OpExecInitInfo exec = OpExecInitInfo.apply(code, "python");
        Preconditions.checkArgument(workers >= 1, "Need at least 1 worker.");
        SchemaPropagationFunc func = SchemaPropagationFunc.apply((Function<Map<PortIdentity, Schema>, Map<PortIdentity, Schema>> & Serializable) inputSchemas -> {
            // Initialize a Java HashMap
            java.util.Map<PortIdentity, Schema> javaMap = new java.util.HashMap<>();

            javaMap.put(operatorInfo().outputPorts().head().id(), sourceSchema());

            // Convert the Java Map to a Scala immutable Map
            return OperatorDescriptorUtils.toImmutableMap(javaMap);
        });
        PhysicalOp physicalOp = PhysicalOp.sourcePhysicalOp(
                        workflowId,
                        executionId,
                        operatorIdentifier(),
                        exec
                )
                .withInputPorts(operatorInfo().inputPorts())
                .withOutputPorts(operatorInfo().outputPorts())
                .withIsOneToManyOp(true)
                .withPropagateSchema(func)
                .withLocationPreference(Option.empty());


        if (workers > 1) {
            return physicalOp
                    .withParallelizable(true)
                    .withSuggestedWorkerNum(workers);
        } else {
            return physicalOp.withParallelizable(false);
        }

    }

    @Override
    public OperatorInfo operatorInfo() {
        return new OperatorInfo(
                "1-out Python UDF",
                "User-defined function operator in Python script",
                OperatorGroupConstants.PYTHON_GROUP(),
                asScala(new ArrayList<InputPort>()).toList(),
                asScala(singletonList(new OutputPort(new PortIdentity(0, false), "", false))).toList(),
                false,
                false,
                true,
                false
        );
    }

    @Override
    public Schema sourceSchema() {
        Schema.Builder outputSchemaBuilder = Schema.builder();

        // for any pythonUDFType, it can add custom output columns (attributes).
        if (columns != null) {
            outputSchemaBuilder.add(asScala(columns)).build();
        }
        return outputSchemaBuilder.build();
    }
}
