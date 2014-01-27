/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sofrecom.validation;

import org.activiti.engine.delegate.DelegateTask;

/**
 *
 * @author z.benrhouma
 */
public class WorkflowValidationDFI implements org.activiti.engine.delegate.TaskListener {


    @Override
    public void notify(DelegateTask execution) {
                Object variableLocal = execution.getVariable("choice");
                execution.setVariable("validationDFIApproved", variableLocal);

    }

}
