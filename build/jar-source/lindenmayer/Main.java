package lindenmayer;

import java.io.FileReader;

import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Application principale du TP1.
 *
 * java -jar lindenmayer.jar fichier.json iterations          (EPS, calcul PostScript)
 * java -jar lindenmayer.jar fichier.json iterations --svg    (SVG, bonus)
 * java -jar lindenmayer.jar fichier.json iterations --ps-java (EPS, calcul Java)
 */
public class Main {

    public static void main(String[] args) {

        if (args.length != 2 && args.length != 3) {
            System.err.println(
                    "Usage: java -jar lindenmayer.jar "
                    + "fichier.json iterations [--svg|--ps-java]");
            System.exit(1);
        }

        try {
            int rounds = Integer.parseInt(args[1]);

            if (rounds < 0) {
                throw new IllegalArgumentException(
                        "iterations negatives");
            }

            boolean svg = args.length == 3 && "--svg".equals(args[2]);
            boolean recursivePostScript =
                    args.length == 2
                    || "--ps-recursive".equals(args[2]);
            boolean javaPostScript =
                    args.length == 3 && "--ps-java".equals(args[2]);

            if (args.length == 3 && !svg && !recursivePostScript
                    && !javaPostScript) {
                throw new IllegalArgumentException(
                        "Format inconnu: " + args[2]);
            }

            JSONObject config;

            try (FileReader reader = new FileReader(args[0])) {
                config = new JSONObject(new JSONTokener(reader));
            }

            if (recursivePostScript) {
                PostScriptRecursive.write(config, args[0], rounds, System.out);
                return;
            }

            // Le choix du format se limite au choix de la tortue.
            VueTurtle turtle = svg
                    ? new SvgTurtle(System.out)
                    : new PostScriptTurtle(System.out);

            LSystem system = new LSystem();
            system.initFromJson(config, turtle); // protected : meme package

            turtle.begin(args[0]);
            turtle.end(system.tell(turtle, system.getAxiom(), rounds));

        } catch (NumberFormatException e) {

            System.err.println("Erreur: nombre d'iterations invalide: " + args[1]);
            System.exit(1);

        } catch (Exception e) {

            // Turtle.action() enveloppe les exceptions (reflexion) :
            // on remonte a la cause d'origine pour un message lisible.
            Throwable cause = e;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }

            System.err.println("Erreur: " + cause.getMessage());
            System.exit(1);
        }
    }
}