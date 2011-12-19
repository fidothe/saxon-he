package net.sf.saxon.serialize;

import java.io.Writer;

/**
 * A class that represents a character as a hexadecimal character reference
 * and writes the result to a supplied Writer
*/

public class HexCharacterReferenceGenerator implements CharacterReferenceGenerator {

    public final static HexCharacterReferenceGenerator THE_INSTANCE = new HexCharacterReferenceGenerator();

    private HexCharacterReferenceGenerator(){}

    public void outputCharacterReference(int charval, Writer writer) throws java.io.IOException {
        writer.write("&#x");
        writer.write(Integer.toHexString(charval));
        writer.write(';');
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