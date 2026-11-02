package lindenmayer;

import java.awt.geom.Rectangle2D;

/**
 * Tortue qui produit un fichier graphique complet.
 * Permet a Main de traiter tous les formats de la meme facon :
 * begin(), dessin par le L-systeme, puis end().
 */
public interface VueTurtle extends Turtle {

    /**
     * Ecrit le debut du fichier.
     * Appele apres l'initialisation de la tortue, avant le dessin.
     *
     * @param title titre du dessin
     */
    void begin(String title);

    /**
     * Termine le dessin et ecrit la fin du fichier.
     *
     * @param bounds plus petit rectangle contenant toutes les positions visitees
     */
    void end(Rectangle2D bounds);
}