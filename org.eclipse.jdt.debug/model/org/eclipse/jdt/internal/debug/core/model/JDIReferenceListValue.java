/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.debug.core.model;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IIndexedValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.jdt.debug.core.IJavaObject;
import org.eclipse.jdt.debug.core.IJavaType;
import org.eclipse.jdt.debug.core.IJavaVariable;
import org.eclipse.jdt.internal.debug.core.HeapWalkingManager;
import org.eclipse.jdt.internal.debug.core.JDIDebugPlugin;
import org.eclipse.jdt.internal.debug.core.logicalstructures.JDIPlaceholderVariable;

import com.ibm.icu.text.MessageFormat;
import com.sun.jdi.ObjectReference;

/**
 * A JDI Value representing a set of references to the root object specified
 * in the constructor.  Used to add a list of all references to an object to
 * various views including the variables view.  The value should belong to
 * a <code>JDIReferenceListVariable</code>.  The children of this value will
 * be <code>JDIReferenceListEntryVariable</code>, each representing one reference
 * to the root object.
 * 
 * @see JDIReferenceListVariable
 * @see JDIReferenceListEntryVariable
 * @since 3.3
 */
public class JDIReferenceListValue extends JDIObjectValue implements IIndexedValue {

	private IJavaObject fRoot;
	
	/**
	 * Constructor, initializes this value with its debug target and root object
	 * @param target The debug target associated with this value
	 * @param root The root object that the elements in the array refer to.
	 */
	public JDIReferenceListValue(IJavaObject root) {
		super((JDIDebugTarget)root.getDebugTarget(), ((JDIObjectValue)root).getUnderlyingObject());
		fRoot = root;
	}
	
	/**
	 * @return all references to the root object as an array of IJavaObjects
	 */
	protected synchronized IJavaObject[] getReferences(){
		try{
			int max = HeapWalkingManager.getDefault().getAllReferencesMaxCount();
			return fRoot.getReferringObjects(max);
		} catch (DebugException e) {
			JDIDebugPlugin.log(e);
			return new IJavaObject[0];
		}
	}
	
	/**
	 * @return whether the references to the root object have been loaded from the vm yet.
	 */
	protected synchronized boolean referencesLoaded(){
		if (fRoot instanceof JDIObjectValue){
			return ((JDIObjectValue)fRoot).isReferencesLoaded();	
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.debug.core.model.JDIValue#getVariables()
	 */
	public IVariable[] getVariables() throws DebugException {
		IJavaObject[] elements = getReferences();
		IVariable[] vars = new JDIPlaceholderVariable[elements.length];
		for (int i = 0; i < elements.length; i++) {
			vars[i] = new JDIReferenceListEntryVariable(MessageFormat.format(JDIDebugModelMessages.JDIReferenceListValue_0, new String[]{Integer.toString(i + 1)}),elements[i]);
		}
		return vars;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.debug.core.model.JDIObjectValue#getUnderlyingObject()
	 */
	public ObjectReference getUnderlyingObject() {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.debug.core.model.JDIValue#hasVariables()
	 */
	public boolean hasVariables() throws DebugException {
		if (referencesLoaded()){
			return getReferences().length > 0;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.debug.core.model.JDIValue#isAllocated()
	 */
	public boolean isAllocated() throws DebugException {
		return fRoot.isAllocated();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.debug.core.model.JDIValue#getJavaType()
	 */
	public IJavaType getJavaType() throws DebugException {
		IJavaType[] javaTypes = getJavaDebugTarget().getJavaTypes(getReferenceTypeName());
		if (javaTypes.length > 0) {
			return javaTypes[0];
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.debug.core.model.JDIValue#getSignature()
	 */
	public String getSignature() throws DebugException {
		return "[Ljava/lang/Object;"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.debug.core.model.JDIObjectValue#getReferenceTypeName()
	 */
	public String getReferenceTypeName() throws DebugException {
		return "java.lang.Object[]"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.debug.core.model.JDIValue#getValueString()
	 */
	public String getValueString() throws DebugException {
		return ""; //$NON-NLS-1$
	}
	
	/**
	 * Returns a string representation of this value intended to be displayed
	 * in the detail pane of views.  Lists the references on separate lines.
	 * 
	 * @return a string representation of this value to display in the detail pane
	 */
	public String getDetailString(){
		StringBuffer buf = new StringBuffer();
		Object[] elements = getReferences();
		if (elements.length == 0){
			buf.append(JDIDebugModelMessages.JDIReferenceListValue_2);
		}
		else{
			buf.append(elements.length);
			if (elements.length == 1){
				buf.append(JDIDebugModelMessages.JDIReferenceListValue_3);
			} else {
				buf.append(JDIDebugModelMessages.JDIReferenceListValue_4);
			}
			for (int i = 0; i < elements.length; i++) {
				buf.append(elements[i] + "\n"); //$NON-NLS-1$
			}
		}
		return buf.toString();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.debug.core.model.JDIValue#toString()
	 */
	public String toString() {
		return JDIDebugModelMessages.JDIReferenceListValue_6 + getUnderlyingValue().toString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.debug.core.model.JDIValue#equals(java.lang.Object)
	 */
	public boolean equals(Object o) {
		// Two JDIReferenceListValues are equal if they both have the same root object.
		if (o instanceof JDIReferenceListValue) {
			JDIReferenceListValue ref = (JDIReferenceListValue) o;
			return ref.fRoot.equals(fRoot);
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.debug.core.model.JDIValue#hashCode()
	 */
	public int hashCode() {
		return getClass().hashCode() + fRoot.hashCode();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IIndexedValue#getInitialOffset()
	 */
	public int getInitialOffset() {
		return 0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IIndexedValue#getSize()
	 */
	public int getSize() throws DebugException {
		return getVariables().length;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IIndexedValue#getVariable(int)
	 */
	public IVariable getVariable(int offset) throws DebugException {
		IVariable[] variables = getVariables();
		if (offset < variables.length) {
			return variables[offset];
		} else {
			requestFailed(JDIDebugModelMessages.JDIReferenceListValue_7, new IndexOutOfBoundsException());
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IIndexedValue#getVariables(int, int)
	 */
	public IVariable[] getVariables(int offset, int length) throws DebugException {
		IVariable[] variables = getVariables();
		if (offset < variables.length && (offset + length) <= variables.length) {
			IJavaVariable[] vars = new IJavaVariable[length];
			System.arraycopy(variables, offset, vars, 0, length);
			return vars;
		} else {
			requestFailed(JDIDebugModelMessages.JDIReferenceListValue_8, new IndexOutOfBoundsException());
			return null;
		}
	}

}