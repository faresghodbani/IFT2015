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

import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.json.JSONObject;

/**
 * Class defining the API to an L-system. 
 * 
 * <p><strong>Setting up the L-system. </strong>
 * The implementation builds its data structures 
 * by calling {@link #addRule(Symbol, String)}, 
 * {@link #setAxiom(String)} and {@link #setAction(char, String)}. 
 * The method {@link #initFromJSON(String, Turtle)} parses an input file.
 * 
 * <p><strong>Using the L-system. </strong>
 * The implementation provides access to symbols as instances of {@link Symbol}, and 
 * to sequences as <code>Iterable&lt;Symbol&gt;</code>. 
 * (Note that implementing classes do not 
 * need to store an actual backing list or array for the iterator, 
 * but could calculate 
 * {@link java.util.Iterator#next() } 
 * on the fly.) As a consequence, they can be read only once. 
 * 
 * <p><strong>Random rules.</strong>Implementing classes will need to 
 * generate pseudorandom numbers in a repeatable manner. 
 * The {@link #resetRnd()} methods is provided to 
 * produce the same random rule choices 
 * in {@link #tell(Turtle, Iterator, int)}.
 * The methods {@link #rndInt(int)} and {@link #rndElement(List)} 
 * produce uniformly distributed random values.
 *
 * 
 * @author Mikl&oacute;s Cs&#369;r&ouml;s
 */
public abstract class AbstractLSystem 
{
    /**
     * Random number generator, initialized at instantiation with a known seed. 
     */
    private final Random rnd;
    
    /**
     * Random seed used for resetting the generator.
     */
    private long seed;
    
    
    /*
     * Instantiation and reinitialization.
     */
    /**
     * Initializes the random-number generator.
     */
    protected AbstractLSystem()
    {
        this((new Random()).nextLong());
    }
    
    /**
     * Initializes the random-number generator with a given seed. 
     * 
     * @param init_random_seed seed used at initialization
     */
    protected AbstractLSystem(long seed)
    {
        this.seed = seed;
        this.rnd = new Random(seed);
    }
    
    /**
     * Resets the random number generator's seed to its initial value
     * (at instantiation).
     */
    protected void resetRnd(){
        this.rnd.setSeed(seed);
    }
    
    /**
     * Random integer between uniformly distributed between 
     * 0 (inclusive) and <var>n</var> (exclusive)
     * 
     * @param n
     * @return integer 0..<var>n</var>-1
     */
    protected final int rndInt(int n) {
    	return rnd.nextInt(n);
    }
    
    /**
     * Sets the random seed
     * 
     * @param seed
     */
    protected final void setSeed(long seed) {
    	this.seed = seed;
    }
    
    /**
     * Retrieves the random seed 
     * @return
     */
    protected final long getSeed() { 
    	return seed;
    }
    
    /**
     * Random, uniformly chosen element from a list.
     * 
     * @param <E> type of elements on the list
     * @param list non-null list 
     * @return a random element from the list
     */
    protected final <E> E rndElement(List<? extends E> list){
    	int i = this.rndInt(list.size());
    	return list.get(i);
    }
    /*
     * Specification of the L-System.
     *
     */
    /**
     * Registers a new character in the alphabet and associates a turtle action with it;
     * expected to be called before {@link #setAxiom(String)} and {@link #addRule(char, String)}
     *  
     * This method is called while parsing the input (specifying the alphabet for the L-system).
     * 
     * The action must correspond to one of the methods in {@link Turtle}: {@link Turtle#draw() }, {@link Turtle#move() }, 
     * {@link Turtle#turnL() }, {@link Turtle#turnR}, {@link Turtle#stay}, {@link Turtle#pop() }, {@link Turtle#push() }. 
     * 
     * @param sym character used in the input to denote this symbol
     * @param action a turtle action
     * @return the corresponding {@link Symbol} in the alphabet    
     * @throws IllegalArgumentException if symbol or action was already associated, or unknown action
     */
    public abstract Symbol setAction(char sym, String action);
    
    
    /**
     * Defines the starting sequence for the L-system. 
     * This method is called when parsing the input, after {@link #setAction(char, String)}
     * was called for every character in the axiom. 
     * 
     * Symbols are encoded by <code>char</code>s as in 
     * {@link #setAction(char, String) }. 
     * 
     * @param str starting sequence
     */
    public abstract void setAxiom(String str);

    
    
    /**
     * Adds a new rule to the grammar. This method is called while parsing the input.
     * Called after the action is setup by {@link #setAction(char, String)}
     * for the characters on both sides of the rule. It is allowed to 
     * add the same rule more than once - each one is stored as an alternative
     * (amplifying its probability). 
     * 
     * @param sym symbol on left-hand side that is rewritten by this rule
     * @param expansion sequence on right-hand side
     */
    public abstract void addRule(char sym, String expansion);
    
 
    /*
     * Using the L-System.
     */
    
    /**
     * Starting sequence.
     * @return starting sequence
     */
    public abstract Iterator<Symbol> getAxiom();
    /**
     * Applies a symbol's rewriting rule. 
     * If no rule was previously stored with {@link #addRule}, then it returns null. If a single rule 
     * was given, it uses the rule's right-hand side. If multiple rules were given ({@link #addRule} called with the same 
     * {@link Symbol} argument more than once), then one of them is chosen randomly. 
     * 
     * @param sym a symbol that would be rewritten. 
     * @return null if no rule, or one of the applicable rules chosen randomly
     */
    public abstract Iterator<Symbol> rewrite(Symbol sym);
    /**
     * Executes the action corresponding to Symbol (action specified by {@link #setAction}) on a given turtle.  
     * 
     * @param turtle used for executing the action
     * @param sym the symbol that needs to be executed 
     */
    public abstract void tell(Turtle turtle, Symbol sym );

//    /**
//     * Calculates the result of multiple rounds of rewriting. Symbols with no reriting rules are simply copied 
//     * at each round. 
//     * 
//     * @param seq starting sequence
//     * @param n number of rounds
//     * @return sequence obtained after rewriting the entire sequence <var>n</var> times 
//     */
//    public abstract Iterator<Symbol> applyRules(Iterator<Symbol> seq, int n);
    
    /**
     * Draws the result after multiple rounds of rewriting, starting from an 
     * arbitrary sequence (the axiom). 
     * Symbols with no rewriting rules are simply copied 
     * at each round. 
     * 
     * @param turtle turtle used for drawing
     * @param seq the starting sequence in round 0
     * @param rounds number of rounds
     * @return bounding box (minimal rectangle covering all visited turtle positions)
     */
    public abstract Rectangle2D tell(Turtle turtle, Iterator<Symbol> seq, int rounds);
        
    /** 
     * Initializes this instance from a file. 
     * Implementing classes should 
     * overwrite by parsing the JSON object and 
     * calling
     * {@link #setAction(char, String)}, {@link #setAxiom(String)}, {@link #addRule(Symbol, String)}
     * for grammar specification and {@link Turtle#setUnits(double, double)}, 
     * {@link Turtle#init(java.awt.geom.Point2D, double)}
     * for turtle drawing setup. 
     */
    protected abstract void initFromJson(JSONObject obj, Turtle turtle);
    
}
