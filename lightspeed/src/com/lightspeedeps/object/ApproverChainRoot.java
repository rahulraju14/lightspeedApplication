/**
 * ApproverChainRoot.java
 */
package com.lightspeedeps.object;

import com.lightspeedeps.model.Approver;

/**
 * Defines the interface for classes that can be the root (anchor) of a chain of
 * Approver objects. Currently the Production, Project, and ApprovalAnchor model
 * classes implement this interface, since they are the beginning points of the
 * production, project, and department approval chains, respectively.
 */
public interface ApproverChainRoot {

	public Approver getApprover();

	public void setApprover(Approver approver);

}
