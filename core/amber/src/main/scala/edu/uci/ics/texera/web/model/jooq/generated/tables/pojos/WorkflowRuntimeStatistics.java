/*
 * This file is generated by jOOQ.
 */
package edu.uci.ics.texera.web.model.jooq.generated.tables.pojos;


import edu.uci.ics.texera.web.model.jooq.generated.tables.interfaces.IWorkflowRuntimeStatistics;

import java.sql.Timestamp;

import org.jooq.types.UInteger;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class WorkflowRuntimeStatistics implements IWorkflowRuntimeStatistics {

    private static final long serialVersionUID = -446937835;

    private UInteger  workflowId;
    private UInteger  executionId;
    private String    operatorId;
    private Timestamp time;
    private UInteger  inputTupleCnt;
    private UInteger  outputTupleCnt;
    private Byte      status;

    public WorkflowRuntimeStatistics() {}

    public WorkflowRuntimeStatistics(IWorkflowRuntimeStatistics value) {
        this.workflowId = value.getWorkflowId();
        this.executionId = value.getExecutionId();
        this.operatorId = value.getOperatorId();
        this.time = value.getTime();
        this.inputTupleCnt = value.getInputTupleCnt();
        this.outputTupleCnt = value.getOutputTupleCnt();
        this.status = value.getStatus();
    }

    public WorkflowRuntimeStatistics(
        UInteger  workflowId,
        UInteger  executionId,
        String    operatorId,
        Timestamp time,
        UInteger  inputTupleCnt,
        UInteger  outputTupleCnt,
        Byte      status
    ) {
        this.workflowId = workflowId;
        this.executionId = executionId;
        this.operatorId = operatorId;
        this.time = time;
        this.inputTupleCnt = inputTupleCnt;
        this.outputTupleCnt = outputTupleCnt;
        this.status = status;
    }

    @Override
    public UInteger getWorkflowId() {
        return this.workflowId;
    }

    @Override
    public void setWorkflowId(UInteger workflowId) {
        this.workflowId = workflowId;
    }

    @Override
    public UInteger getExecutionId() {
        return this.executionId;
    }

    @Override
    public void setExecutionId(UInteger executionId) {
        this.executionId = executionId;
    }

    @Override
    public String getOperatorId() {
        return this.operatorId;
    }

    @Override
    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    @Override
    public Timestamp getTime() {
        return this.time;
    }

    @Override
    public void setTime(Timestamp time) {
        this.time = time;
    }

    @Override
    public UInteger getInputTupleCnt() {
        return this.inputTupleCnt;
    }

    @Override
    public void setInputTupleCnt(UInteger inputTupleCnt) {
        this.inputTupleCnt = inputTupleCnt;
    }

    @Override
    public UInteger getOutputTupleCnt() {
        return this.outputTupleCnt;
    }

    @Override
    public void setOutputTupleCnt(UInteger outputTupleCnt) {
        this.outputTupleCnt = outputTupleCnt;
    }

    @Override
    public Byte getStatus() {
        return this.status;
    }

    @Override
    public void setStatus(Byte status) {
        this.status = status;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("WorkflowRuntimeStatistics (");

        sb.append(workflowId);
        sb.append(", ").append(executionId);
        sb.append(", ").append(operatorId);
        sb.append(", ").append(time);
        sb.append(", ").append(inputTupleCnt);
        sb.append(", ").append(outputTupleCnt);
        sb.append(", ").append(status);

        sb.append(")");
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // FROM and INTO
    // -------------------------------------------------------------------------

    @Override
    public void from(IWorkflowRuntimeStatistics from) {
        setWorkflowId(from.getWorkflowId());
        setExecutionId(from.getExecutionId());
        setOperatorId(from.getOperatorId());
        setTime(from.getTime());
        setInputTupleCnt(from.getInputTupleCnt());
        setOutputTupleCnt(from.getOutputTupleCnt());
        setStatus(from.getStatus());
    }

    @Override
    public <E extends IWorkflowRuntimeStatistics> E into(E into) {
        into.from(this);
        return into;
    }
}