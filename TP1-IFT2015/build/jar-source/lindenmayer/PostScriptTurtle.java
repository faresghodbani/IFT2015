package lindenmayer;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.PrintStream;
import java.util.Locale;

/**
 * Tortue produisant un dessin EPS (partie e).
 * L'etat est gere par SimpleTurtle ; cette classe ajoute la sortie PostScript.
 */
public class PostScriptTurtle extends SimpleTurtle implements VueTurtle {

    private final PrintStream out;

    public PostScriptTurtle(PrintStream out) {
        this.out = out;
    }

    /**
     * Ecrit la position courante suivie d'un operateur PostScript.
     * Locale.US : sinon une locale francaise ecrirait "1,5" au lieu de "1.5".
     */
    private void point(String prefix, String op) {
        Point2D p = getPosition();
        out.printf(Locale.US, "%s%.4f %.4f %s%n",
                prefix, p.getX(), p.getY(), op);
    }

    @Override
    public void draw() {
        super.draw();
        point("", "lineto");
    }

    @Override
    public void move() {
        super.move();
        point("", "moveto");
    }

    @Override
    public void push() {
        super.push();
        // stroke efface la position courante : currentpoint la garde sur la pile
        out.println("currentpoint stroke newpath moveto");
    }

    @Override
    public void pop() {
        super.pop();
        point("stroke newpath ", "moveto");
    }

    @Override
    public void begin(String title) {
        out.println("%!PS-Adobe-3.0 EPSF-3.0");
        out.println("%%Title: ("
                + title.replace('(', '_').replace(')', '_').replace('\\', '_')
                + ")");
        out.println("%%Creator: (" + getClass().getName() + ")");
        out.println("%%BoundingBox: (atend)");
        out.println("%%EndComments");
        out.println("1 setlinecap 1 setlinejoin");
        point("newpath ", "moveto");
    }

    @Override
    public void end(Rectangle2D bbox) {
        out.println("stroke");
        out.println("%%Trailer");

        // floor/ceil pour contenir tout le dessin (aussi en negatif),
        // marge de 1 pour l'epaisseur du trait.
        out.printf("%%%%BoundingBox: %d %d %d %d%n",
                (int) Math.floor(bbox.getMinX()) - 1,
                (int) Math.floor(bbox.getMinY()) - 1,
                (int) Math.ceil(bbox.getMaxX()) + 1,
                (int) Math.ceil(bbox.getMaxY()) + 1);

        out.println("%%EOF");
        out.flush();
    }
}