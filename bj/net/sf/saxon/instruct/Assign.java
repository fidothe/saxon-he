package net.sf.saxon.instruct;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.ValueRepresentation;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.Closure;
import net.sf.saxon.value.SequenceExtent;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.Value;

/**
* saxon:assign element in stylesheet.
*
* The saxon:assign element has mandatory attribute name and optional attribute expr.
* It also allows xsl:extension-element-prefixes etc.
*/

public class Assign extends GeneralVariable implements BindingReference {


    private Binding binding;    // link to the variable declaration

    public Assign() {}

    public void setStaticType(SequenceType type, Value constantValue, int properties) {}

    public void fixup(Binding binding) {
        this.binding = binding;
    }


    public int getIntrinsicDependencies() {
        return StaticProperty.HAS_SIDE_EFFECTS;
    }    

    /**
     * Offer promotion for this subexpression. This needs careful handling in the
     * case of saxon:assign
     *
     * @param offer details of the offer, for example the offer to move
     *     expressions that don't depend on the context to an outer level in
     *     the containing expression
     * @exception net.sf.saxon.trans.XPathException if any error is detected
     * @return if the offer is not accepted, return this expression unchanged.
     *      Otherwise return the result of rewriting the expression to promote
     *      this subexpression
     */

    public Expression promote(PromotionOffer offer) throws XPathException {
        switch (offer.action) {
            case PromotionOffer.RANGE_INDEPENDENT:
            case PromotionOffer.FOCUS_INDEPENDENT:
                return this;

            case PromotionOffer.REPLACE_CURRENT:
            case PromotionOffer.INLINE_VARIABLE_REFERENCES:
            case PromotionOffer.UNORDERED:
                return super.promote(offer);

            default:
                throw new UnsupportedOperationException("Unknown promotion action " + offer.action);
        }
    }



    /**
    * Get the name of this instruction for diagnostic and tracing purposes
    */

    public int getInstructionNameCode() {
        return StandardNames.SAXON_ASSIGN;
    }


    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        if (binding==null) {
            throw new IllegalStateException("saxon:assign binding has not been fixed up");
        }
        ValueRepresentation value = getSelectValue(context);
        if (value instanceof Closure) {
            value = SequenceExtent.makeSequenceExtent(((Closure)value).iterate(null));
        }
        if (binding instanceof GeneralVariable) {
            if (binding.isGlobal()) {
                context.getController().getBindery().assignGlobalVariable((GlobalVariable)binding, value);
            } else {
                throw new UnsupportedOperationException("Local variables are not assignable");
            }
        } else {

        }
        return null;
    }

    /**
     * Evaluate the variable (method exists only to satisfy the interface)
     */

    public ValueRepresentation evaluateVariable(XPathContext context) throws XPathException {
        throw new UnsupportedOperationException();
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
