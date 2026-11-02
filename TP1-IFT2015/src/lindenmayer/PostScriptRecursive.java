package lindenmayer;

import java.awt.geom.Rectangle2D;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Produit un EPS dont les regles de reecriture sont executees par PostScript.
 */
final class PostScriptRecursive {

    private static final String[] ACTIONS = {
        "draw", "move", "turnL", "turnR", "push", "pop", "stay"
    };

    private final PrintStream out;
    private final JSONObject config;
    private final String title;
    private final int rounds;
    private final Map<Character, Integer> symbolIds = new LinkedHashMap<>();
    private final Map<Character, String> actions = new LinkedHashMap<>();
    private final Map<Character, List<List<Character>>> rules =
            new LinkedHashMap<>();
    private final List<Character> axiom = new ArrayList<>();
    private double step;
    private double angle;
    private double startX;
    private double startY;
    private double startAngle;

    private PostScriptRecursive(
            JSONObject config, String title, int rounds, PrintStream out) {
        this.title = title;
        this.rounds = rounds;
        this.out = out;
        this.config = config;
        readConfig(config);
    }

    static void write(
            JSONObject config, String title, int rounds, PrintStream out) {
        new PostScriptRecursive(config, title, rounds, out).write();
    }

    private void readConfig(JSONObject config) {
        JSONObject actionObject = config.getJSONObject("actions");

        for (String code : actionObject.keySet()) {
            char symbol = singleChar(code);
            String action = actionObject.getString(code);

            if (!isAction(action)) {
                throw new IllegalArgumentException(
                        "action inconnue: " + action);
            }

            if (actions.put(symbol, action) != null) {
                throw new IllegalArgumentException(
                        "symbole deja declare: " + symbol);
            }

            symbolIds.put(symbol, symbolIds.size());
        }

        readSequence(config.getString("axiom"), axiom);

        JSONObject ruleObject = config.getJSONObject("rules");
        for (String code : ruleObject.keySet()) {
            char symbol = declaredSymbol(code);
            JSONArray alternatives = ruleObject.getJSONArray(code);
            List<List<Character>> expansions = new ArrayList<>();

            for (int i = 0; i < alternatives.length(); i++) {
                List<Character> expansion = new ArrayList<>();
                readSequence(alternatives.getString(i), expansion);
                expansions.add(expansion);
            }

            if (!expansions.isEmpty()) {
                rules.put(symbol, expansions);
            }
        }

        JSONObject parameters = config.getJSONObject("parameters");
        step = parameters.getDouble("step");
        angle = parameters.getDouble("angle");
        JSONArray start = parameters.getJSONArray("start");

        if (start.length() != 3) {
            throw new IllegalArgumentException(
                    "start doit contenir x, y, angle");
        }

        startX = start.getDouble(0);
        startY = start.getDouble(1);
        startAngle = start.getDouble(2);

        if (!Double.isFinite(step) || step <= 0
                || !Double.isFinite(angle)
                || !Double.isFinite(startX)
                || !Double.isFinite(startY)
                || !Double.isFinite(startAngle)) {
            throw new IllegalArgumentException(
                    "parametres de tortue invalides");
        }
    }

    private void readSequence(String sequence, List<Character> result) {
        for (int i = 0; i < sequence.length(); i++) {
            char symbol = sequence.charAt(i);
            if (!symbolIds.containsKey(symbol)) {
                throw new IllegalArgumentException(
                        "symbole non declare: " + symbol);
            }
            result.add(symbol);
        }
    }

    private char declaredSymbol(String code) {
        char symbol = singleChar(code);
        if (!symbolIds.containsKey(symbol)) {
            throw new IllegalArgumentException(
                    "symbole gauche non declare: " + symbol);
        }
        return symbol;
    }

    private static char singleChar(String code) {
        if (code.length() != 1) {
            throw new IllegalArgumentException(
                    "code symbole invalide: " + code);
        }
        return code.charAt(0);
    }

    private static boolean isAction(String action) {
        for (String validAction : ACTIONS) {
            if (validAction.equals(action)) {
                return true;
            }
        }
        return false;
    }

    private void write() {
        out.println("%!PS-Adobe-3.0 EPSF-3.0");
        out.println("%%Title: (" + safeTitle(title) + ")");
        out.println("%%Creator: (lindenmayer.PostScriptRecursive)");
        out.println("%%LanguageLevel: 2");
        out.println("%%BoundingBox: (atend)");
        out.println("%%EndComments");
        out.println("1 setlinecap 1 setlinejoin");
        writeTurtleActions();
        writeRuleChooser();

        for (char symbol : symbolIds.keySet()) {
            writeSymbolProcedure(symbol);
        }

        out.println("gsave");
        out.println(number(startX) + " " + number(startY) + " translate");
        out.println(number(startAngle) + " rotate");

        for (char symbol : axiom) {
            out.println(rounds + " S" + symbolIds.get(symbol));
        }

        out.println("stroke");
        out.println("grestore");
        out.println("%%Trailer");
        writeBoundingBox();
        out.println("%%EOF");
        out.flush();
    }

    private void writeTurtleActions() {
        out.printf(Locale.US,
                "/T:draw { newpath 0 0 moveto %s 0 lineto "
                + "stroke %s 0 translate } bind def%n",
                number(step), number(step));
        out.printf(Locale.US,
                "/T:move { %s 0 translate } bind def%n", number(step));
        out.printf(Locale.US,
                "/T:turnL { %s rotate } bind def%n", number(angle));
        out.printf(Locale.US,
                "/T:turnR { %s rotate } bind def%n", number(-angle));
        out.println("/T:push { gsave } bind def");
        out.println("/T:pop { grestore } bind def");
        out.println("/T:stay { } bind def");
    }

    private void writeRuleChooser() {
        // Rejection sampling avoids modulo bias in the random rule choice.
        out.println("/T:choose {");
        out.println("  1 dict begin");
        out.println("    /n exch def");
        out.println("    /limit 2147483647 n idiv n mul def");
        out.println("    {");
        out.println("      /r rand def");
        out.println("      r limit lt { r n mod exit } if");
        out.println("    } loop");
        out.println("  end");
        out.println("} bind def");
    }

    private void writeSymbolProcedure(char symbol) {
        int id = symbolIds.get(symbol);
        List<List<Character>> alternatives = rules.get(symbol);
        String action = actions.get(symbol);

        out.println("/S" + id + " {");
        if (alternatives == null) {
            out.println("  pop T:" + action);
            out.println("} bind def");
            return;
        }

        out.println("  dup 0 le { pop T:" + action + " } {");
        out.println("    1 sub 1 dict begin /d exch def");
        writeRuleSelection(alternatives);

        out.println("    end");
        out.println("  } ifelse");
        out.println("} bind def");
    }

    private void writeRuleSelection(List<List<Character>> alternatives) {
        out.printf("    %d T:choose%n", alternatives.size());

        for (int i = 0; i < alternatives.size() - 1; i++) {
            out.printf("    dup %d eq { pop%n", i);
            writeExpansion(alternatives.get(i));
            out.println("    } {");
        }

        out.println("    pop");
        writeExpansion(alternatives.get(alternatives.size() - 1));

        for (int i = 0; i < alternatives.size() - 1; i++) {
            out.println("    } ifelse");
        }
    }

    private void writeExpansion(List<Character> expansion) {
        for (char symbol : expansion) {
            out.println("    d S" + symbolIds.get(symbol));
        }
    }

    private void writeBoundingBox() {
        Rectangle2D exactBounds = exactBoundingBoxIfSmall();
        if (exactBounds != null) {
            out.printf("%%%%BoundingBox: %d %d %d %d%n",
                    (int) Math.floor(exactBounds.getMinX()) - 1,
                    (int) Math.floor(exactBounds.getMinY()) - 1,
                    (int) Math.ceil(exactBounds.getMaxX()) + 1,
                    (int) Math.ceil(exactBounds.getMaxY()) + 1);
            return;
        }

        // Count forward actions without expanding the exponentially growing word.
        BigInteger countLimit = BigDecimal.valueOf(1.0e9)
                .divide(BigDecimal.valueOf(step), 0, RoundingMode.CEILING)
                .toBigInteger();
        List<Character> symbols = new ArrayList<>(symbolIds.keySet());
        Map<Character, BigInteger> counts = new LinkedHashMap<>();

        for (char symbol : symbols) {
            counts.put(symbol, isForwardAction(actions.get(symbol))
                    ? BigInteger.ONE : BigInteger.ZERO);
        }

        Map<List<BigInteger>, Integer> seen = new HashMap<>();
        int round = 0;
        while (round < rounds) {
            List<BigInteger> state = new ArrayList<>(counts.values());
            Integer previousRound = seen.putIfAbsent(state, round);

            if (previousRound != null) {
                int cycleLength = round - previousRound;
                int skippedRounds = (rounds - round) / cycleLength;
                if (skippedRounds > 0) {
                    round += skippedRounds * cycleLength;
                    continue;
                }
            }

            Map<Character, BigInteger> nextCounts = new LinkedHashMap<>();
            for (char symbol : symbols) {
                List<List<Character>> alternatives = rules.get(symbol);

                if (alternatives == null) {
                    nextCounts.put(symbol, isForwardAction(actions.get(symbol))
                            ? BigInteger.ONE : BigInteger.ZERO);
                    continue;
                }

                BigInteger maximum = BigInteger.ZERO;
                for (List<Character> expansion : alternatives) {
                    BigInteger total = BigInteger.ZERO;
                    for (char child : expansion) {
                        total = total.add(counts.get(child));
                        if (total.compareTo(countLimit) >= 0) {
                            total = countLimit;
                            break;
                        }
                    }
                    if (total.compareTo(maximum) > 0) {
                        maximum = total;
                    }
                }
                nextCounts.put(symbol, maximum);
            }

            counts = nextCounts;
            round++;
        }

        BigInteger forwardCount = BigInteger.ZERO;
        for (char symbol : axiom) {
            forwardCount = forwardCount.add(counts.get(symbol));
            if (forwardCount.compareTo(countLimit) >= 0) {
                forwardCount = countLimit;
                break;
            }
        }

        double distance = new BigDecimal(forwardCount)
                .multiply(BigDecimal.valueOf(step))
                .doubleValue();

        if (forwardCount.compareTo(countLimit) >= 0
                || Math.abs(startX) + distance > 2.0e9
                || Math.abs(startY) + distance > 2.0e9) {
            out.println("%%BoundingBox: -2147483647 -2147483647 "
                    + "2147483647 2147483647");
            return;
        }

        int minX = (int) Math.floor(startX - distance) - 1;
        int minY = (int) Math.floor(startY - distance) - 1;
        int maxX = (int) Math.ceil(startX + distance) + 1;
        int maxY = (int) Math.ceil(startY + distance) + 1;

        out.printf("%%%%BoundingBox: %d %d %d %d%n",
                minX, minY, maxX, maxY);
    }

    private Rectangle2D exactBoundingBoxIfSmall() {
        if (rounds > 256) {
            return null;
        }

        for (List<List<Character>> alternatives : rules.values()) {
            if (alternatives.size() > 1) {
                return null;
            }
        }

        BigInteger limit = BigInteger.valueOf(1_000_000);
        Map<Character, BigInteger> counts = new LinkedHashMap<>();
        for (char symbol : symbolIds.keySet()) {
            counts.put(symbol, BigInteger.ONE);
        }

        for (int round = 0; round < rounds; round++) {
            Map<Character, BigInteger> nextCounts = new LinkedHashMap<>();
            for (char symbol : symbolIds.keySet()) {
                List<List<Character>> alternatives = rules.get(symbol);
                if (alternatives == null) {
                    nextCounts.put(symbol, BigInteger.ONE);
                    continue;
                }

                BigInteger count = BigInteger.ZERO;
                for (char child : alternatives.get(0)) {
                    count = count.add(counts.get(child));
                    if (count.compareTo(limit) > 0) {
                        count = limit.add(BigInteger.ONE);
                        break;
                    }
                }
                nextCounts.put(symbol, count);
            }
            counts = nextCounts;
        }

        BigInteger sequenceLength = BigInteger.ZERO;
        for (char symbol : axiom) {
            sequenceLength = sequenceLength.add(counts.get(symbol));
            if (sequenceLength.compareTo(limit) > 0) {
                return null;
            }
        }

        LSystem system = new LSystem(0);
        SimpleTurtle turtle = new SimpleTurtle();
        system.initFromJson(config, turtle);
        return system.tell(turtle, system.getAxiom(), rounds);
    }

    private static boolean isForwardAction(String action) {
        return "draw".equals(action) || "move".equals(action);
    }

    private static String number(double value) {
        String formatted = BigDecimal.valueOf(value)
                .stripTrailingZeros()
                .toPlainString();
        return formatted.indexOf('.') >= 0 ? formatted : formatted + ".0";
    }

    private static String safeTitle(String title) {
        return title.replace('(', '_').replace(')', '_')
                .replace('\\', '_').replace('\n', '_')
                .replace('\r', '_');
    }
}
