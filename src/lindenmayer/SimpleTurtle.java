package lindenmayer;

import java.awt.geom.Point2D;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Tortue sans dessin (partie a).
 * Gere la position, l'orientation et la pile d'etats, mais ne dessine rien.
 * Sert de classe de base aux tortues qui produisent une sortie
 * (PostScriptTurtle, SvgTurtle).
 */
public class SimpleTurtle implements Turtle {

    /**
     * Etat sauvegarde par push() : position et angle.
     */
    private static class State {
        final double x;
        final double y;
        final double angle;

        State(double x, double y, double angle) {
            this.x = x;
            this.y = y;
            this.angle = angle;
        }
    }

    /* Etat par defaut (javadoc de Turtle) : position (0,0), angle 0. */
    private double x = 0;
    private double y = 0;
    private double angle = 0;
    private double step = 1;
    private double delta = 30;

    /** Pile des etats ; ArrayDeque plutot que Stack (non synchronisee). */
    private final Deque<State> stack = new ArrayDeque<>();

    @Override
    public void draw() {
        advance();
    }

    @Override
    public void move() {
        advance();
    }

    /**
     * Avance d'un pas dans la direction du nez.
     * Methode privee : draw() et move() ne s'appellent pas l'une l'autre,
     * donc une sous-classe peut redefinir l'une sans affecter l'autre.
     */
    private void advance() {
        double radians = Math.toRadians(angle);

        x += step * Math.cos(radians);
        y += step * Math.sin(radians);
    }

    @Override
    public void turnR() {
        angle -= delta;
    }

    @Override
    public void turnL() {
        angle += delta;
    }

    @Override
    public void push() {
        stack.push(new State(x, y, angle));
    }

    /**
     * @throws IllegalStateException si la pile est vide (crochets mal balances)
     */
    @Override
    public void pop() {
        if (stack.isEmpty()) {
            throw new IllegalStateException("pop sans push");
        }

        State s = stack.pop();

        x = s.x;
        y = s.y;
        angle = s.angle;
    }

    @Override
    public void init(Point2D pos, double angle) {
        if (pos == null) {
            throw new IllegalArgumentException("position null");
        }

        x = pos.getX();
        y = pos.getY();
        this.angle = angle;

        stack.clear();
    }

    @Override
    public Point2D getPosition() {
        // Copie : l'appelant ne peut pas modifier l'etat de la tortue.
        return new Point2D.Double(x, y);
    }

    @Override
    public double getAngle() {
        return angle;
    }

    @Override
    public void setUnits(double step, double delta) {
        if (!Double.isFinite(step) || step <= 0
                || !Double.isFinite(delta)) {
            throw new IllegalArgumentException("unites invalides");
        }

        this.step = step;
        this.delta = delta;
    }

    @Override
    public double getUnitStep() {
        return step;
    }

    @Override
    public double getUnitAngle() {
        return delta;
    }
}