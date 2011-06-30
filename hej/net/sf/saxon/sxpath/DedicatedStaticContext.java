package net.sf.saxon.sxpath;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.Container;
import net.sf.saxon.expr.instruct.Executable;
import net.sf.saxon.functions.FunctionLibraryList;
import net.sf.saxon.om.StructuredQName;

import java.util.HashMap;

/**
 *
 */
public class DedicatedStaticContext extends IndependentContext implements Container {

    private Executable executable;

    public DedicatedStaticContext(Configuration config) {
        super(config);
    }

    /**
     * Create a DedicatedStaticContext as a copy of an IndependentContext
     * @param ic the IndependentContext to be copied
     */

    public DedicatedStaticContext(IndependentContext ic) {
        super(ic.getConfiguration());
        setBaseURI(ic.getBaseURI());
        setLocationMap(ic.getLocationMap());
        setDefaultElementNamespace(ic.getDefaultElementNamespace());
        setDefaultFunctionNamespace(ic.getDefaultFunctionNamespace());
        setBackwardsCompatibilityMode(ic.isInBackwardsCompatibleMode());
        setSchemaAware(ic.isSchemaAware());
        namespaces = new HashMap<String, String>(ic.namespaces);
        variables = new HashMap<StructuredQName, XPathVariable>(10);
        FunctionLibraryList libList = (FunctionLibraryList)ic.getFunctionLibrary();
        if (libList != null) {
            setFunctionLibrary((FunctionLibraryList)libList.copy());
        }
        importedSchemaNamespaces = ic.importedSchemaNamespaces;
        externalResolver = ic.externalResolver;
        autoDeclare = ic.autoDeclare;
        setXPathLanguageLevel(ic.getXPathLanguageLevel());
        requiredContextItemType = ic.requiredContextItemType;
        if (ic instanceof DedicatedStaticContext) {
            setExecutable(((DedicatedStaticContext)ic).getExecutable());
        }
    }

    public void setExecutable(Executable exec) {
        executable = exec;
    }

    public Executable getExecutable() {
        return executable;
    }
}

//
// The contents of this file are subject to the Mozilla Public License Version
// 1.0 (the "License");
// you may not use this file except in compliance with the License. You may
// obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations
// under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael Kay,
//
// Portions created by (your name) are Copyright (C) (your legal entity). All
// Rights Reserved.
//
// Contributor(s): none.
//



