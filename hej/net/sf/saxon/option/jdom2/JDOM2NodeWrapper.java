////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2013 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.option.jdom2;

import net.sf.saxon.event.Receiver;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.AnyNodeTest;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.*;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.tree.util.Navigator;
import net.sf.saxon.tree.wrapper.AbstractNodeWrapper;
import net.sf.saxon.tree.wrapper.SiblingCountingNode;
import net.sf.saxon.type.Type;
import org.jdom2.*;
import org.jdom2.filter.ElementFilter;
import org.jdom2.located.Located;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * A node in the XML parse tree representing an XML element, character content, or attribute.<P>
 * This is the implementation of the NodeInfo interface used as a wrapper for JDOM2 nodes.
 *
 * @author Michael H. Kay
 */

public class JDOM2NodeWrapper extends AbstractNodeWrapper implements SiblingCountingNode {

    protected Object node;          // the JDOM node to which this XPath node is mapped; or a List of
    // adjacent text nodes
    protected short nodeKind;
    private JDOM2NodeWrapper parent;     // null means unknown
    protected JDOM2DocumentWrapper docWrapper;
    protected int index;            // -1 means unknown

    /**
     * This constructor is protected: nodes should be created using the wrap
     * factory method on the DocumentWrapper class
     *
     * @param node   The JDOM node to be wrapped
     * @param parent The NodeWrapper that wraps the parent of this node
     * @param index  Position of this node among its siblings
     */
    protected JDOM2NodeWrapper(Object node, JDOM2NodeWrapper parent, int index) {
        this.node = node;
        this.parent = parent;
        this.index = index;
    }

    /**
     * Factory method to wrap a JDOM node with a wrapper that implements the Saxon
     * NodeInfo interface.
     *
     * @param node       The JDOM node
     * @param docWrapper The wrapper for the Document containing this node
     * @return The new wrapper for the supplied node
     */
    protected JDOM2NodeWrapper makeWrapper(Object node, JDOM2DocumentWrapper docWrapper) {
        return makeWrapper(node, docWrapper, null, -1);
    }

    /**
     * Factory method to wrap a JDOM node with a wrapper that implements the Saxon
     * NodeInfo interface.
     *
     * @param node       The JDOM node
     * @param docWrapper The wrapper for the Document containing this node
     * @param parent     The wrapper for the parent of the JDOM node
     * @param index      The position of this node relative to its siblings
     * @return The new wrapper for the supplied node
     */

    protected JDOM2NodeWrapper makeWrapper(Object node, JDOM2DocumentWrapper docWrapper,
                                           JDOM2NodeWrapper parent, int index) {
        JDOM2NodeWrapper wrapper;
        if (node instanceof Document) {
            return docWrapper;
        } else if (node instanceof Element) {
            wrapper = new JDOM2NodeWrapper(node, parent, index);
            wrapper.nodeKind = Type.ELEMENT;
        } else if (node instanceof Attribute) {
            wrapper = new JDOM2NodeWrapper(node, parent, index);
            wrapper.nodeKind = Type.ATTRIBUTE;
        } else if (node instanceof String || node instanceof Text) {
            wrapper = new JDOM2NodeWrapper(node, parent, index);
            wrapper.nodeKind = Type.TEXT;
        } else if (node instanceof Comment) {
            wrapper = new JDOM2NodeWrapper(node, parent, index);
            wrapper.nodeKind = Type.COMMENT;
        } else if (node instanceof ProcessingInstruction) {
            wrapper = new JDOM2NodeWrapper(node, parent, index);
            wrapper.nodeKind = Type.PROCESSING_INSTRUCTION;
        } else if (node instanceof Namespace) {
            throw new IllegalArgumentException("Cannot wrap JDOM namespace objects");
        } else {
            throw new IllegalArgumentException("Bad node type in JDOM! " + node.getClass() + " instance " + node.toString());
        }
        wrapper.docWrapper = docWrapper;
        return wrapper;
    }

    /**
     * Get the underlying JDOM node, to implement the VirtualNode interface
     */

    public Object getUnderlyingNode() {
        if (node instanceof List) {
            return ((List) node).get(0);
        } else {
            return node;
        }
    }

    /**
     * Return the type of node.
     *
     * @return one of the values Node.ELEMENT, Node.TEXT, Node.ATTRIBUTE, etc.
     */

    public int getNodeKind() {
        return nodeKind;
    }

    /**
     * Determine the relative position of this node and another node, in document order.
     * The other node will always be in the same document.
     *
     * @param other The other node, whose position is to be compared with this node
     * @return -1 if this node precedes the other node, +1 if it follows the other
     *         node, or 0 if they are the same node. (In this case, isSameNode() will always
     *         return true, and the two nodes will produce the same result for generateId())
     */

    public int compareOrder(NodeInfo other) {
        if (other instanceof SiblingCountingNode) {
            return Navigator.compareOrder(this, (SiblingCountingNode) other);
        } else {
            // it must be a namespace node
            return -other.compareOrder(this);
        }
    }

    /**
     * Get the value of the item as a CharSequence. This is in some cases more efficient than
     * the version of the method that returns a String.
     */

    public CharSequence getStringValueCS() {
        if (node instanceof List) {
            // This wrapper is mapped to a list of adjacent text nodes
            List nodes = (List) node;
            FastStringBuffer fsb = new FastStringBuffer(FastStringBuffer.SMALL);
            for (Object node1 : nodes) {
                Text o = (Text) node1;
                fsb.append(getStringValue(o));
            }
            return fsb;
        } else {
            return getStringValue(node);
        }
    }

    @Override
    public int getLineNumber() {
        if (node instanceof Located) {
            return ((Located)node).getLine();
        } else {
            return -1;
        }
    }

    @Override
    public int getColumnNumber() {
        if (node instanceof Located) {
            return ((Located)node).getColumn();
        } else {
            return -1;
        }
    }


    /**
     * Supporting method to get the string value of a node
     *
     * @param node the JDOM node
     * @return the XPath string value of the node
     */

    private static String getStringValue(Object node) {
        if (node instanceof Document) {
            List children1 = ((Document) node).getContent();
            FastStringBuffer sb1 = new FastStringBuffer(FastStringBuffer.MEDIUM);
            expandStringValue(children1, sb1);
            return sb1.toString();
        } else if (node instanceof Element) {
            return ((Element) node).getValue();
        } else if (node instanceof Attribute) {
            return ((Attribute) node).getValue();
        } else if (node instanceof Text) {
            return ((Text) node).getText();
        } else if (node instanceof String) {
            return (String) node;
        } else if (node instanceof Comment) {
            return ((Comment) node).getText();
        } else if (node instanceof ProcessingInstruction) {
            return ((ProcessingInstruction) node).getData();
        } else if (node instanceof Namespace) {
            return ((Namespace) node).getURI();
        } else {
            return "";
        }
    }

    /**
     * Get the string values of all the nodes in a list, concatenating the values into
     * a supplied string buffer
     *
     * @param list the list containing the nodes
     * @param sb   the StringBuffer to contain the result
     */
    private static void expandStringValue(List list, FastStringBuffer sb) {
        for (Object obj : list) {
            if (obj instanceof Element) {
                sb.append(((Element) obj).getValue());
            } else if (obj instanceof Text) {
                sb.append(((Text) obj).getText());
            } else if (obj instanceof EntityRef) {
                throw new IllegalStateException("Unexpanded entity in JDOM tree");
            } else if (obj instanceof DocType) {
                // do nothing: can happen in JDOM beta 10
            } else {
                throw new AssertionError("Unknown JDOM node type");
            }
        }
    }

    /**
     * Get the local part of the name of this node. This is the name after the ":" if any.
     *
     * @return the local part of the name. For an unnamed node, returns "".
     */

    public String getLocalPart() {
        switch (nodeKind) {
            case Type.ELEMENT:
                return ((Element) node).getName();
            case Type.ATTRIBUTE:
                return ((Attribute) node).getName();
            case Type.TEXT:
            case Type.COMMENT:
            case Type.DOCUMENT:
                return "";
            case Type.PROCESSING_INSTRUCTION:
                return ((ProcessingInstruction) node).getTarget();
            case Type.NAMESPACE:
                return ((Namespace) node).getPrefix();
            default:
                return null;
        }
    }

    /**
     * Get the prefix part of the name of this node. This is the name before the ":" if any.
     * (Note, this method isn't required as part of the NodeInfo interface.)
     *
     * @return the prefix part of the name. For an unnamed node, return an empty string.
     */

    public String getPrefix() {
        switch (nodeKind) {
            case Type.ELEMENT:
                return ((Element) node).getNamespacePrefix();
            case Type.ATTRIBUTE:
                return ((Attribute) node).getNamespacePrefix();
            default:
                return "";
        }
    }

    /**
     * Get the URI part of the name of this node. This is the URI corresponding to the
     * prefix, or the URI of the default namespace if appropriate.
     *
     * @return The URI of the namespace of this node. For an unnamed node,
     *         or for a node with an empty prefix, return an empty
     *         string.
     */

    public String getURI() {
        switch (nodeKind) {
            case Type.ELEMENT:
                return ((Element) node).getNamespaceURI();
            case Type.ATTRIBUTE:
                return ((Attribute) node).getNamespaceURI();
            default:
                return "";
        }
    }

    /**
     * Get the display name of this node. For elements and attributes this is [prefix:]localname.
     * For unnamed nodes, it is an empty string.
     *
     * @return The display name of this node.
     *         For a node with no name, return an empty string.
     */

    public String getDisplayName() {
        switch (nodeKind) {
            case Type.ELEMENT:
                return ((Element) node).getQualifiedName();
            case Type.ATTRIBUTE:
                return ((Attribute) node).getQualifiedName();
            case Type.PROCESSING_INSTRUCTION:
            case Type.NAMESPACE:
                return getLocalPart();
            default:
                return "";

        }
    }

    /**
     * Get the NodeInfo object representing the parent of this node
     */

    public NodeInfo getParent() {
        if (parent == null) {
            if (node instanceof Element) {
                if (((Element) node).isRootElement()) {
                    parent = makeWrapper(((Element) node).getDocument(), docWrapper);
                } else {
                    parent = makeWrapper(((Element) node).getParent(), docWrapper);
                }
            } else if (node instanceof Text) {
                parent = makeWrapper(((Text) node).getParent(), docWrapper);
            } else if (node instanceof Comment) {
                parent = makeWrapper(((Comment) node).getParent(), docWrapper);
            } else if (node instanceof ProcessingInstruction) {
                parent = makeWrapper(((ProcessingInstruction) node).getParent(), docWrapper);
            } else if (node instanceof Attribute) {
                parent = makeWrapper(((Attribute) node).getParent(), docWrapper);
            } else if (node instanceof Document) {
                parent = null;
            } else if (node instanceof Namespace) {
                throw new UnsupportedOperationException("Cannot find parent of JDOM namespace node");
            } else {
                throw new IllegalStateException("Unknown JDOM node type " + node.getClass());
            }
        }
        return parent;
    }

    /**
     * Get the index position of this node among its siblings (starting from 0)
     * In the case of a text node that maps to several adjacent siblings in the JDOM,
     * the numbering actually refers to the position of the underlying JDOM nodes;
     * thus the sibling position for the text node is that of the first JDOM node
     * to which it relates, and the numbering of subsequent XPath nodes is not necessarily
     * consecutive.
     */

    public int getSiblingPosition() {
        if (index == -1) {
            int ix = 0;
            getParent();
            if (parent == null) {
                return 0;
            }
            AxisIterator iter;
            switch (nodeKind) {
                case Type.ELEMENT:
                case Type.TEXT:
                case Type.COMMENT:
                case Type.PROCESSING_INSTRUCTION:
                    iter = parent.iterateAxis(AxisInfo.CHILD);
                    break;
                case Type.ATTRIBUTE:
                    iter = parent.iterateAxis(AxisInfo.ATTRIBUTE);
                    break;
                case Type.NAMESPACE:
                    iter = parent.iterateAxis(AxisInfo.NAMESPACE);
                    break;
                default:
                    index = 0;
                    return index;
            }
            while (true) {
                NodeInfo n = iter.next();
                if (n == null) {
                    break;
                }
                if (n.isSameNodeInfo(this)) {
                    index = ix;
                    return index;
                }
                if (((JDOM2NodeWrapper) n).node instanceof List) {
                    ix += ((List) (((JDOM2NodeWrapper) n).node)).size();
                } else {
                    ix++;
                }
            }
            throw new IllegalStateException("JDOM node not linked to parent node");
        }
        return index;
    }

    @Override
    protected AxisIterator<NodeInfo> iterateAttributes(NodeTest nodeTest) {
        AxisIterator<NodeInfo> base = new AttributeEnumeration(this);
        if (nodeTest == AnyNodeTest.getInstance()) {
            return base;
        } else {
            return new Navigator.AxisFilter(base, nodeTest);
        }
    }

    @Override
    protected AxisIterator<NodeInfo> iterateChildren(NodeTest nodeTest) {
        if (hasChildNodes()) {
            AxisIterator<NodeInfo> base = new ChildEnumeration(this, true, true);
            if (nodeTest == AnyNodeTest.getInstance()) {
                return base;
            } else {
                return new Navigator.AxisFilter(base, nodeTest);
            }
        } else {
            return EmptyAxisIterator.emptyAxisIterator();
        }
    }

    @Override
    protected AxisIterator<NodeInfo> iterateSiblings(NodeTest nodeTest, boolean forwards) {
        if (nodeTest == AnyNodeTest.getInstance()) {
            return new ChildEnumeration(this, false, forwards);
        } else {
            return new Navigator.AxisFilter(
                            new ChildEnumeration(this, false, forwards),
                            nodeTest);
        }
    }

    @Override
    protected AxisIterator<NodeInfo> iterateDescendants(NodeTest nodeTest, boolean includeSelf) {
        Iterator descendants;
        if (nodeTest.getNodeKindMask() == 1<<Type.ELEMENT) {
            // only select element nodes
            descendants = ((Parent)node).getDescendants(new ElementFilter());
        } else {
            descendants = ((Parent)node).getDescendants();
        }
        NodeWrappingFunction wrappingFunct = new NodeWrappingFunction<Content, NodeInfo>() {
            public NodeInfo wrap(Content node) {
                return makeWrapper(node, docWrapper);
            }
        };
        AxisIterator<NodeInfo> wrappedDescendants = new DescendantWrappingIterator(descendants, wrappingFunct);

        if (includeSelf && nodeTest.matches(this)) {
            wrappedDescendants = new PrependIterator(this, wrappedDescendants);
        }

        if (nodeTest instanceof AnyNodeTest || ((nodeTest instanceof NodeKindTest && ((NodeKindTest)nodeTest).getNodeKind() == Type.ELEMENT))) {
            return wrappedDescendants;
        } else {
            return new Navigator.AxisFilter(wrappedDescendants, nodeTest);
        }
    }

    private static class DescendantWrappingIterator extends NodeWrappingAxisIterator {

        public DescendantWrappingIterator(Iterator descendantIterator, NodeWrappingFunction wrappingFunction) {
            super(descendantIterator, wrappingFunction);
        }

        public DescendantWrappingIterator getAnother() {
            return new DescendantWrappingIterator(getBaseIterator(), getNodeWrappingFunction());
        }

        @Override
        public boolean isIgnorable(Object node) {
            return node instanceof DocType;
        }
    }

    /**
     * Get the string value of a given attribute of this node
     *
     * @param uri   the namespace URI of the attribute name. Supply the empty string for an attribute
     *              that is in no namespace
     * @param local the local part of the attribute name.
     * @return the attribute value if it exists, or null if it does not exist. Always returns null
     *         if this node is not an element.
     * @since 8.4
     */


    public String getAttributeValue( String uri,  String local) {
        if (nodeKind == Type.ELEMENT) {
            return ((Element) node).getAttributeValue(local,
                    (uri.equals(NamespaceConstant.XML) ?
                            Namespace.XML_NAMESPACE :
                            Namespace.getNamespace(uri)));
            // JDOM doesn't allow getNamespace() on the XML namespace URI
        }
        return null;
    }

    /**
     * Get the root node - always a document node with this tree implementation
     *
     * @return the NodeInfo representing the containing document
     */

    public NodeInfo getRoot() {
        return docWrapper;
    }

    /**
     * Get the root (document) node
     *
     * @return the DocumentInfo representing the containing document
     */

    public DocumentInfo getDocumentRoot() {
        return docWrapper;
    }

    /**
     * Determine whether the node has any children. <br />
     * Note: the result is equivalent to <br />
     * getEnumeration(AxisInfo.CHILD, AnyNodeTest.getInstance()).hasNext()
     */

    public boolean hasChildNodes() {
        switch (nodeKind) {
            case Type.DOCUMENT:
                return true;
            case Type.ELEMENT:
                return !((Element) node).getContent().isEmpty();
            default:
                return false;
        }
    }

    /**
     * Get a character string that uniquely identifies this node.
     * Note: a.isSameNode(b) if and only if generateId(a)==generateId(b)
     *
     * @param buffer a Buffer to contain a string that uniquely identifies this node, across all
     *               documents
     */

    public void generateId(FastStringBuffer buffer) {
        Navigator.appendSequentialKey(this, buffer, true);
        //buffer.append(Navigator.getSequentialKey(this));
    }

    /**
     * Get the document number of the document containing this node. For a free-standing
     * orphan node, just return the hashcode.
     */

    public long getDocumentNumber() {
        return getDocumentRoot().getDocumentNumber();
    }

    /**
     * Copy this node to a given outputter (deep copy)
     */

    public void copy(Receiver out, int copyOptions, int locationId) throws XPathException {
        Navigator.copy(this, out, copyOptions, locationId);
    }

    /**
     * Get all namespace undeclarations and undeclarations defined on this element.
     *
     * @param buffer If this is non-null, and the result array fits in this buffer, then the result
     *               may overwrite the contents of this array, to avoid the cost of allocating a new array on the heap.
     * @return An array of integers representing the namespace declarations and undeclarations present on
     *         this element. For a node other than an element, return null. Otherwise, the returned array is a
     *         sequence of namespace codes, whose meaning may be interpreted by reference to the name pool. The
     *         top half word of each namespace code represents the prefix, the bottom half represents the URI.
     *         If the bottom half is zero, then this is a namespace undeclaration rather than a declaration.
     *         The XML namespace is never included in the list. If the supplied array is larger than required,
     *         then the first unused entry will be set to -1.
     *         <p/>
     *         <p>For a node other than an element, the method returns null.</p>
     */

    public NamespaceBinding[] getDeclaredNamespaces(NamespaceBinding[] buffer) {
        if (node instanceof Element) {
            Element elem = (Element) node;
            List addl = elem.getAdditionalNamespaces();
            int size = addl.size() + 1;
            NamespaceBinding[] result = (buffer == null || size > buffer.length ? new NamespaceBinding[size] : buffer);
            NamePool pool = getNamePool();
            Namespace ns = elem.getNamespace();
            String prefix = ns.getPrefix();
            String uri = ns.getURI();
            result[0] = new NamespaceBinding(prefix, uri);
            int i = 1;
            if (!addl.isEmpty()) {
                for (Object anAddl : addl) {
                    ns = (Namespace) anAddl;
                    result[i++] = new NamespaceBinding(ns.getPrefix(), ns.getURI());
                }
            }
            if (size < result.length) {
                result[size] = null;
            }
            return result;
        } else {
            return null;
        }
    }

    /**
     * Determine whether this node has the is-id property
     *
     * @return true if the node is an ID
     */

    public boolean isId() {
        return node instanceof Attribute && ((Attribute) node).getAttributeType() == Attribute.ID_TYPE;
    }

    /**
     * Determine whether this node has the is-idref property
     *
     * @return true if the node is an IDREF or IDREFS element or attribute
     */

    public boolean isIdref() {
        if (node instanceof Attribute) {
            AttributeType type = ((Attribute)node).getAttributeType();
            return type == Attribute.IDREF_TYPE || type == Attribute.IDREFS_TYPE;
        } else {
            return false;
        }
    }


    ///////////////////////////////////////////////////////////////////////////////
    // Axis enumeration classes
    ///////////////////////////////////////////////////////////////////////////////


    private final class AttributeEnumeration extends Navigator.BaseEnumeration {

        private Iterator atts;
        private int ix = 0;
        private JDOM2NodeWrapper start;

        public AttributeEnumeration(JDOM2NodeWrapper start) {
            this.start = start;
            atts = ((Element) start.node).getAttributes().iterator();
        }

        public void advance() {
            if (atts.hasNext()) {
                current = makeWrapper(atts.next(), docWrapper, start, ix++);
            } else {
                current = null;
            }
        }

        
        public AxisIterator getAnother() {
            return new AttributeEnumeration(start);
        }

    }  // end of class AttributeEnumeration


    /**
     * The class ChildEnumeration handles not only the child axis, but also the
     * following-sibling and preceding-sibling axes. It can also iterate the children
     * of the start node in reverse order, something that is needed to support the
     * preceding and preceding-or-ancestor axes (the latter being used by xsl:number)
     */

    private final class ChildEnumeration extends Navigator.BaseEnumeration {

        private JDOM2NodeWrapper start;
        private JDOM2NodeWrapper commonParent;
        private ListIterator children;
        private int ix = 0;
        private boolean downwards;  // iterate children of start node (not siblings)
        private boolean forwards;   // iterate in document order (not reverse order)

        public ChildEnumeration(JDOM2NodeWrapper start,
                                boolean downwards, boolean forwards) {
            this.start = start;
            this.downwards = downwards;
            this.forwards = forwards;

            if (downwards) {
                commonParent = start;
            } else {
                commonParent = (JDOM2NodeWrapper) start.getParent();
            }

            if (commonParent.getNodeKind() == Type.DOCUMENT) {
                children = ((Document) commonParent.node).getContent().listIterator();
            } else {
                children = ((Element) commonParent.node).getContent().listIterator();
            }

            if (downwards) {
                if (!forwards) {
                    // backwards enumeration: go to the end
                    while (children.hasNext()) {
                        children.next();
                        ix++;
                    }
                }
            } else {
                ix = start.getSiblingPosition();
                // find the start node among the list of siblings
                Object n = null;
                if (forwards) {
                    for (int i = 0; i <= ix; i++) {
                        n = children.next();
                    }
                    if (n instanceof Text) {
                        // move to the last of a sequence of adjacent text nodes
                        boolean atEnd = false;
                        while (n instanceof Text) {
                            if (children.hasNext()) {
                                n = children.next();
                                ix++;
                            } else {
                                atEnd = true;
                                break;
                            }
                        }
                        if (!atEnd) {
                            children.previous();
                        }
                    } else {
                        ix++;
                    }
                } else {
                    for (int i = 0; i < ix; i++) {
                        children.next();
                    }
                    ix--;
                }
            }
        }

        public void advance() {
            if (forwards) {
                if (children.hasNext()) {
                    Object nextChild = children.next();
                    if (nextChild instanceof DocType) {
                        advance();
                        return;
                    }
                    if (nextChild instanceof EntityRef) {
                        throw new IllegalStateException("Unexpanded entity in JDOM tree");
                    } else if (nextChild instanceof Text) {
                        current = makeWrapper(nextChild, docWrapper, commonParent, ix++);
                        List list = null;
                        while (children.hasNext()) {
                            Object n = children.next();
                            if (n instanceof Text) {
                                if (list == null) {
                                    list = new ArrayList(4);
                                    list.add(((JDOM2NodeWrapper) current).node);
                                }
                                list.add(n);
                                ix++;
                            } else {
                                // we've looked ahead too far
                                children.previous();
                                break;
                            }
                        }
                        if (list != null) {
                            ((JDOM2NodeWrapper) current).node = list;
                        }
                    } else {
                        current = makeWrapper(nextChild, docWrapper, commonParent, ix++);
                    }
                } else {
                    current = null;
                }
            } else {    // backwards
                if (children.hasPrevious()) {
                    Object nextChild = children.previous();
                    if (nextChild instanceof DocType) {
                        advance();
                        return;
                    }
                    if (nextChild instanceof EntityRef) {
                        throw new IllegalStateException("Unexpanded entity in JDOM tree");
                    } else if (nextChild instanceof Text) {
                        current = makeWrapper(nextChild, docWrapper, commonParent, ix--);
                        List list = null;
                        while (children.hasPrevious()) {
                            Object n = children.previous();
                            if (n instanceof Text) {
                                if (list == null) {
                                    list = new ArrayList(4);
                                    list.add(((JDOM2NodeWrapper) current).node);
                                }
                                list.add(0, n);
                                ix--;
                            } else {
                                // we've looked ahead too far
                                children.next();
                                break;
                            }
                        }
                        if (list != null) {
                            ((JDOM2NodeWrapper) current).node = list;
                        }
                    } else {
                        current = makeWrapper(nextChild, docWrapper, commonParent, ix--);
                    }
                } else {
                    current = null;
                }
            }
        }

        
        public AxisIterator getAnother() {
            return new ChildEnumeration(start, downwards, forwards);
        }

    } // end of class ChildEnumeration

}


// Original Code is Copyright (c) Saxonica Limited 2009. All rights reserved.