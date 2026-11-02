/*
 * MIT License
 * 
 * Copyright (c) 2026 Miklós Csűrös
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package lindenmayer;

/**
 * Symbol in an L-system's alphabet. 
 * 
 * @author Mikl&oacute;s Cs&#369;r&ouml;s
 */
public class Symbol {
    private final char value;
    public Symbol(char c){
        this.value = c;
    }
    
    @Override
    public String toString(){
        return Character.toString(value);
    }
    
    @Override
    public int hashCode() {
    	return Character.hashCode(value);
    }
     
    /**
     * Equality by character value
     */
    @Override 
    public boolean equals(Object o) {
    	if (o instanceof Symbol) { // false if o is null
    		Symbol that = (Symbol) o;
    		return this.value == that.value;
    	} else
    		return super.equals(o); // false
    }
   
}