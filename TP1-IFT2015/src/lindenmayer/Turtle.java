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

import java.awt.geom.Point2D;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Turtle graphics interface. The turtle state is defined as its 
 * location on the plane and the orientation of its nose. 
 * Implementing classes are expected to initialize 
 * the turtle with position (0,0) and angle 0 by default.
 * The turtle moves and draws by unit-length steps, and turns left or right by a unit angle
 * (e.g., 30), which are set in {@link #setUnits(double, double) }. 
 * 
 * 
 * @author Mikl&oacute;s Cs&#369;r&ouml;s
 */
public interface Turtle {
    /**
     * Draws a line of unit length
     */
    public void draw();
    /**
     * Moves by unit length, no drawing. 
     */
    public void move();
    /**
     * Turn right (clockwise) by unit angle.
     */
    public void turnR();
    /**
     * Turn left (counter-clockwise) by unit angle.
     */
    public void turnL();
    /**
     * Saves turtle state
     */
    public void push();
    /**
     * Recovers turtle state
     */
    public void pop();
    /**
     * Lets the turtle relax; does not change state. 
     */
    public default void stay() {}
    /**
     * Initializes the turtle state (and clears the state stack)
     * @param pos turtle position
     * @param angle angle in degrees (90=up, 0=right)
     */
    public void init(Point2D pos, double angle);
    /**
     * Turtle position 
     * 
     * @return location of the turtle on the plane
     */
    public Point2D getPosition();
    /**
     * angle of the turtle's nose
     * @return angle in degrees
     */
    public double getAngle();
    /**
     * sets the unit step and unit angle
     * 
     * @param step length of an advance (move or draw)
     * @param delta unit angle change in degrees (for turnR and turnL)
     */
    public void setUnits(double step, double delta);
    
    /**
     * Unit step.
     * @return
     */
    public double getUnitStep();
    /**
     * Unit angle in degrees.
     * @return
     */
    public double getUnitAngle();
    

    /**
     * Access to the interface by String method name:
     * draw, move, turnR, turnL, push, pop, stay.
	 * Creates a Runnable for which the only method {@link Runnable#run()} 
	 * corresponds to the turtle's method of this name.
     * 
     * @param name
     * @return a Runnable that executes the method with this turtle
     */
    public default Runnable action(String name) {
    	final Method method;
    	try {
			method = this.getClass().getMethod(name, (Class[]) null);
    	}  catch (NoSuchMethodException e) {
    		throw new IllegalArgumentException("Unrecognized action name "+name); // bad method name    		
    	}
    	
    	return new Runnable() {
    		@Override
    		public void run() {
    			try {
        	        method.invoke(Turtle.this, (Object[])null);
    	        } catch (IllegalAccessException | InvocationTargetException e){
        	        throw new RuntimeException(e); // exception when invoking the method
    	        }
    		}
    	};
    }
}