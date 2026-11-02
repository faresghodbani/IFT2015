package lindenmayer;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Grammaire de Lindenmayer et interpretation recursive (parties b, c et d).
 *
 * Structures de donnees :
 * - alphabet : une seule instance de Symbol par caractere ;
 * - actions  : nom de l'action de tortue associee a chaque symbole ;
 * - rules    : pour chaque symbole, la liste de ses regles (chaque regle est
 *              la liste des symboles du cote droit). Les doublons sont gardes,
 *              ce qui augmente la probabilite de la regle.
 */
public class LSystem extends AbstractLSystem {

    private static final List<String> VALID_ACTIONS =
            Arrays.asList("draw", "move", "turnL", "turnR", "push", "pop", "stay");

    private final Map<Character, Symbol> alphabet =
            new HashMap<>();

    private final Map<Symbol, String> actions =
            new HashMap<>();

    private final Map<Symbol, List<List<Symbol>>> rules =
            new HashMap<>();

    private List<Symbol> axiom = new ArrayList<>();

    /** Systeme vide, graine aleatoire. */
    public LSystem() {
        super();
    }

    /** Systeme vide, graine donnee (dessins reproductibles). */
    public LSystem(long seed) {
        super(seed);
    }

    /* ------------------------------------------------------------ */
    /* Specification du systeme                                      */
    /* ------------------------------------------------------------ */

    @Override
    public Symbol setAction(char c, String action) {

        if (alphabet.containsKey(c)) {
            throw new IllegalArgumentException(
                    "symbole deja declare: " + c);
        }

        if (!VALID_ACTIONS.contains(action)) {
            throw new IllegalArgumentException(
                    "action inconnue: " + action);
        }

        Symbol sym = new Symbol(c);

        alphabet.put(c, sym);
        actions.put(sym, action);

        return sym;
    }

    /**
     * Convertit une chaine en liste de symboles deja declares.
     *
     * @throws IllegalArgumentException si un caractere n'a pas ete declare
     */
    private List<Symbol> symbols(String text) {

        List<Symbol> result = new ArrayList<>();

        for (int i = 0; i < text.length(); i++) {

            Symbol sym = alphabet.get(text.charAt(i));

            if (sym == null) {
                throw new IllegalArgumentException(
                        "symbole non declare: " + text.charAt(i));
            }

            result.add(sym);
        }

        return result;
    }

    @Override
    public void setAxiom(String text) {
        axiom = symbols(text);
    }

    @Override
    public void addRule(char c, String expansion) {

        Symbol sym = alphabet.get(c);

        if (sym == null) {
            throw new IllegalArgumentException(
                    "symbole gauche non declare: " + c);
        }

        rules.computeIfAbsent(sym, k -> new ArrayList<>())
                .add(symbols(expansion));
    }

    /* ------------------------------------------------------------ */
    /* Acces aux regles et execution                                 */
    /* ------------------------------------------------------------ */

    @Override
    public Iterator<Symbol> getAxiom() {
        return axiom.iterator(); // nouvel iterateur a chaque appel
    }

    @Override
    public Iterator<Symbol> rewrite(Symbol sym) {

        List<List<Symbol>> alternatives = rules.get(sym);

        if (alternatives == null || alternatives.isEmpty()) {
            return null; // symbole terminal
        }

        return rndElement(alternatives).iterator();
    }

    @Override
    public void tell(Turtle turtle, Symbol sym) {

        String action = actions.get(sym);

        if (action == null) {
            throw new IllegalArgumentException(
                    "action absente: " + sym);
        }

        turtle.action(action).run();
    }

    /* ------------------------------------------------------------ */
    /* Inference + dessin                                            */
    /* ------------------------------------------------------------ */

    /**
     * Execute la chaine obtenue apres rounds reecritures de seq.
     * La chaine complete n'est jamais construite : chaque symbole est
     * reecrit recursivement au moment ou on le rencontre. La memoire
     * utilisee depend de la profondeur (rounds), pas de la taille du dessin.
     *
     * @return plus petit rectangle contenant toutes les positions visitees
     */
    @Override
    public Rectangle2D tell(
            Turtle turtle,
            Iterator<Symbol> seq,
            int rounds) {

        if (rounds < 0) {
            throw new IllegalArgumentException(
                    "rounds negatif");
        }

        resetRnd();

        Point2D start = turtle.getPosition();
        Rectangle2D bbox = new Rectangle2D.Double(
                start.getX(), start.getY(), 0, 0);

        expand(turtle, seq, rounds, bbox);

        return bbox;
    }

    /**
     * Recursion : a la profondeur 0 on execute les symboles ; sinon chaque
     * symbole est remplace par une de ses regles, developpee avec une
     * ronde de moins. Un symbole terminal ne change jamais : on l'execute
     * directement.
     *
     * @param bbox rectangle agrandi sur place a chaque position visitee
     */
    private void expand(
            Turtle turtle,
            Iterator<Symbol> seq,
            int rounds,
            Rectangle2D bbox) {

        while (seq.hasNext()) {

            Symbol sym = seq.next();
            Iterator<Symbol> next = (rounds == 0) ? null : rewrite(sym);

            if (next == null) {
                tell(turtle, sym);
                bbox.add(turtle.getPosition());
            } else {
                expand(turtle, next, rounds - 1, bbox);
            }
        }
    }

    /* ------------------------------------------------------------ */
    /* Initialisation par fichier JSON                               */
    /* ------------------------------------------------------------ */

    /**
     * Initialise le systeme et la tortue a partir d'un objet JSON avec les
     * cles actions, axiom, rules et parameters (step, angle, start=[x,y,angle]).
     * Les actions sont lues en premier, car l'axiome et les regles les utilisent.
     */
    @Override
    protected void initFromJson(
            JSONObject obj, Turtle turtle) {

        JSONObject acts = obj.getJSONObject("actions");

        for (String code : acts.keySet()) {
            setAction(singleChar(code), acts.getString(code));
        }

        setAxiom(obj.getString("axiom"));

        JSONObject allRules = obj.getJSONObject("rules");

        for (String code : allRules.keySet()) {

            char c = singleChar(code);
            JSONArray alternatives = allRules.getJSONArray(code);

            for (int i = 0; i < alternatives.length(); i++) {
                addRule(c, alternatives.getString(i));
            }
        }

        JSONObject parameters = obj.getJSONObject("parameters");
        JSONArray start = parameters.getJSONArray("start");

        if (start.length() != 3) {
            throw new IllegalArgumentException(
                    "start doit contenir x, y, angle");
        }

        turtle.setUnits(
                parameters.getDouble("step"),
                parameters.getDouble("angle"));

        turtle.init(
                new Point2D.Double(
                        start.getDouble(0),
                        start.getDouble(1)),
                start.getDouble(2));
    }

    /**
     * Verifie qu'une cle JSON est un seul caractere.
     */
    private static char singleChar(String code) {
        if (code.length() != 1) {
            throw new IllegalArgumentException(
                    "code symbole invalide: " + code);
        }
        return code.charAt(0);
    }
}