package net.sf.saxon.style;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.instruct.CopyOf;
import net.sf.saxon.expr.instruct.Executable;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.lib.Validation;
import net.sf.saxon.om.AttributeCollection;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.value.Whitespace;


/**
* An xsl:copy-of element in the stylesheet. <br>
*/

public final class XSLCopyOf extends StyleElement {

    /*@Nullable*/ private Expression select;
    private boolean copyNamespaces;
    private int validation = Validation.PRESERVE;
    private SchemaType schemaType;
    private boolean readOnce = false;       // extension attribute to enable serial processing

    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction
    */

    public boolean isInstruction() {
        return true;
    }

    public void prepareAttributes() throws XPathException {

		AttributeCollection atts = getAttributeList();
		String selectAtt = null;
		String copyNamespacesAtt = null;
		String validationAtt = null;
		String typeAtt = null;
        String readOnceAtt = null;

		for (int a=0; a<atts.getLength(); a++) {
			String f = atts.getQName(a);
			if (f.equals(StandardNames.SELECT)) {
        		selectAtt = atts.getValue(a);
            } else if (f.equals(StandardNames.COPY_NAMESPACES)) {
                copyNamespacesAtt = Whitespace.trim(atts.getValue(a));
            } else if (f.equals(StandardNames.VALIDATION)) {
                validationAtt = Whitespace.trim(atts.getValue(a));
            } else if (f.equals(StandardNames.TYPE)) {
                typeAtt = Whitespace.trim(atts.getValue(a));
            } else if (f.equals("read-once") && atts.getURI(a).equals(NamespaceConstant.SAXON)) {
                readOnceAtt = Whitespace.trim(atts.getValue(a));
        	} else {
        		checkUnknownAttribute(atts.getNodeName(a));
        	}
        }

        if (selectAtt!=null) {
            select = makeExpression(selectAtt);
        } else {
            reportAbsence("select");
        }

        if (copyNamespacesAtt == null) {
            copyNamespaces = true;
        } else {
            checkAttributeValue("copy-namespaces", copyNamespacesAtt, false, StyleElement.YES_NO);
            copyNamespaces = (copyNamespacesAtt.equals("yes"));
        }

        if (validationAtt!=null) {
            validation = Validation.getCode(validationAtt);
            if (validation != Validation.STRIP && !getPreparedStylesheet().isSchemaAware()) {
                validation = Validation.STRIP;
                compileError("To perform validation, a schema-aware XSLT processor is needed", "XTSE1660");
            }
            if (validation == Validation.INVALID) {
                compileError("invalid value of validation attribute", "XTSE0020");
            }
        } else {
            validation = getContainingStylesheet().getDefaultValidation();
        }

        if (typeAtt!=null) {
            schemaType = getSchemaType(typeAtt);
            if (!getPreparedStylesheet().isSchemaAware()) {
                compileError("The @type attribute is available only with a schema-aware XSLT processor", "XTSE1660");
            }
            validation = Validation.BY_TYPE;
        }

        if (typeAtt != null && validationAtt != null) {
            compileError("The @validation and @type attributes are mutually exclusive", "XTSE1505");
        }

        if (readOnceAtt != null) {
            checkAttributeValue("saxon:read-once", readOnceAtt, false, StyleElement.YES_NO);
            readOnce = (readOnceAtt.equals("yes"));
        }
    }

    public void validate(Declaration decl) throws XPathException {
        checkEmpty();
        select = typeCheck("select", select);
    }

    public Expression compile(Executable exec, Declaration decl) {
        CopyOf inst = new CopyOf(select, copyNamespaces, validation, schemaType, false);
        if (readOnce) {
            exec.getConfiguration().checkLicensedFeature(Configuration.LicenseFeature.ENTERPRISE_XSLT, "streaming");
            inst.setReadOnce(readOnce);
        }
        inst.setCopyLineNumbers(exec.getConfiguration().isLineNumbering());
        inst.setStaticBaseUri(getBaseURI());
        return inst;
    }

}

//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Saxonica Limited.
// Portions created by ___ are Copyright (C) ___. All rights reserved.
//
// Contributor(s):
//