package esa.sen2vm;

/**
 * Main class
 *
 */
public class App
{
    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );

        if (args.length < 2) {
            System.out.println("Usage: java Main -c <mandatoryArg> [-p optionalArg]");
            return;
        }

        String mandatoryArg = null;
        String optionalArg = null;

        for (int i = 0; i < args.length; i++) {
            if ("-c".equals(args[i])) {
                if (i + 1 < args.length) {
                    mandatoryArg = args[i + 1];
                }
            } else if (i > 0 && args[i - 1].equals("-p")) {
                optionalArg = args[i];
            }
        }

        if (mandatoryArg == null) {
            System.out.println("Usage: java Main -c <mandatoryArg> [-p optionalArg]");
            return;
        }

        System.out.println("Mandatory Argument: " + mandatoryArg);
        if (optionalArg != null) {
            System.out.println("Optional Argument: " + optionalArg);
        } else {
            System.out.println("No Optional Argument provided");
        }

    }
}
