package lindenmayer;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.PrintStream;
import java.util.Locale;

/**
 * Tortue qui genere un dessin SVG (bonus g).
 *
 * Le dessin est un seul element path : L pour tracer, M pour se deplacer
 * sans tracer (move et pop). Le chemin est garde en memoire, car le viewBox
 * (connu seulement a la fin) doit etre ecrit avant lui.
 * En SVG l'axe y pointe vers le bas : on ecrit donc -y.
 */
public class SvgTurtle extends SimpleTurtle implements VueTurtle {

    /** Plus grand cote de l'image, en pixels. */
    private static final double SIZE = 800;

    private final PrintStream out;
    /** Chemin SVG ; commence par M a la position par defaut (0,0). */
    private final StringBuilder path = new StringBuilder("M0 0 ");

    public SvgTurtle(PrintStream out) {
        this.out = out;
    }

    private void command(char code) {
        Point2D p = getPosition();

        path.append(String.format(
                Locale.US,
                "%c%.3f %.3f ",
                code,
                p.getX(),
                -p.getY()));
    }

    @Override
    public void init(Point2D position, double angle) {
        super.init(position, angle);

        path.setLength(0);
        command('M');
    }

    @Override
    public void draw() {
        super.draw();
        command('L');
    }

    @Override
    public void move() {
        super.move();
        command('M');
    }

    @Override
    public void pop() {
        super.pop();
        command('M');
    }

    /** Rien a ecrire avant le dessin : l'en-tete depend du viewBox. */
    @Override
    public void begin(String title) {
    }

    @Override
    public void end(Rectangle2D bounds) {

        double margin = Math.max(1.0, getUnitStep());

        double x = bounds.getMinX() - margin;
        double y = -bounds.getMaxY() - margin;
        double width = Math.max(1.0, bounds.getWidth()) + 2 * margin;
        double height = Math.max(1.0, bounds.getHeight()) + 2 * margin;

        // Taille en pixels proportionnelle au dessin.
        double scale = SIZE / Math.max(width, height);

        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

        out.printf(Locale.US,
                "<svg xmlns=\"http://www.w3.org/2000/svg\" "
                + "viewBox=\"%.3f %.3f %.3f %.3f\" "
                + "width=\"%.0f\" height=\"%.0f\">%n",
                x, y, width, height,
                width * scale, height * scale);

        out.printf(Locale.US,
                "  <rect x=\"%.3f\" y=\"%.3f\" "
                + "width=\"%.3f\" height=\"%.3f\" fill=\"white\"/>%n",
                x, y, width, height);

        // non-scaling-stroke : epaisseur de 1 pixel, quelle que soit l'echelle.
        out.print("  <path fill=\"none\" stroke=\"black\" "
                + "stroke-width=\"1\" vector-effect=\"non-scaling-stroke\" "
                + "stroke-linecap=\"round\" stroke-linejoin=\"round\" d=\"");
        out.print(path);
        out.println("\"/>");
        out.println("</svg>");
        out.flush();
    }
}