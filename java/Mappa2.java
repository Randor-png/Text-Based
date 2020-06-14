import java.io.*;
import java.util.Scanner;
import java.util.InputMismatchException;
import java.util.*;  
import java.io.File;

public class Mappa2
{
    private Scanner movement = new Scanner(System.in);         //This is for the Movement input
    private Scanner choose = new Scanner(System.in);           //This is for Choice input
    private Scanner pause = new Scanner(System.in);         //This is to pause text screens

    private FileReader readermaps;  //Reads the Map file
    private FileReader readerevents;    //Reads the Event file
    private FileReader readercommevents;    //Reads the Common Event file
    private FileReader readertiles;     //Reads the Tileset file
    //private FileReader readercolors;    //Reads the Color file [DEACTIVATED. DOESN'T WORK OUTSIDE OF MACOS]

    private Properties propmaps;
    private Properties propevents;
    private Properties propcommevents;
    private Properties proptiles;
    //private Properties propcolors;

    private String tiles;   //Contains the tilesets and their properties
    //private String colors;  //The colors used on the map [DEACTIVATED. DOESN'T WORK OUTSIDE OF MACOS]
    //private String reset = "\u001B[0m"; //Resets text color to white
    private String touchevents;
    private String runevents;
    private String stepevents;

    private String home; //The route for the files
    private String fs; //The separators in the route

    private char map[][];   //Contains the map
    private int mapx;       //The map's length
    private int mapy;       //The map's height
    //private String mapcolors;
    private int mapid = 0;                      //Used to remember on which map the Player is on
    private int[] var = new int[200];           //Variables applicable on every map
    private int[][] mapvar = new int[50][200];    //Local variables that work only in their respective map

    private char playericon = '$';  //The character representing the Player
    private char steptile = '.';    //The default "replace" tile used to fill the Player's old position after he moves

    private boolean ifCheck = true;    //used to check if the Condition of an IF or WHEN Command is satisfied or not
    private boolean loopCheck = false; //used to check if it is necessary to commit a Loop
    private int ifSkip = -1;    //The ID of the IF Command. We keep it so that we can skip its content if necessary
    private int elseSkip = -1;  //The ID of the ELSE Command. We keep it so that we can skip its content if necessary
    private int loopID = -1;    //The ID of the LOOP Command. We keep it so that we can skip its content if necessary


	public Mappa2()
	{
        home = System.getProperty("user.dir");
        home = home.substring(0, home.length()-5);
        fs = File.separator;

        try
        {
            readertiles = new FileReader(new File(home + fs + "data" + fs + "tiles.txt"));
            //readercolors = new FileReader(new File(home + fs + "data" + fs + "colors.txt"));
            readercommevents = new FileReader(new File(home + fs + "data" + fs + "mapevents" + fs + "commonevents.txt"));
        }
        catch(FileNotFoundException fnfe)
        {
        }

        //propcolors = new Properties();
        proptiles = new Properties();
        propcommevents = new Properties();

        try
        {
            proptiles.load(readertiles);
            //propcolors.load(readercolors);
            propcommevents.load(readercommevents);
        }
        catch(IOException ioe)
        {
        }

        tiles = proptiles.getProperty("tiles");
        //colors = propcolors.getProperty("color");

        mapid = 0;
        changeMap(mapid, 5, 8);
    }

    //This function is call every time the Player is moving in another Map
    public void changeMap(int mapid, int px, int py)
    {
        int i = 0;
        int j = 0;

        //We retrieve the Map and Events File of the Map the Player is going in

        try
        {
            readermaps = new FileReader(new File(home + fs + "data" + fs + "maps" + fs + "MAP_" + mapid + ".txt"));
            readerevents = new FileReader(new File(home + fs + "data" + fs + "mapevents" + fs + "EVENTS_" + mapid + ".txt"));

        }
        catch(FileNotFoundException fnfe)
        {
        }

        propmaps = new Properties();
        propevents = new Properties();
        

        try
        {
            propmaps.load(readermaps);
            proptiles.load(readertiles);
            propevents.load(readerevents);
            //propcolors.load(readercolors);
        }
        catch(IOException ioe)
        {
        }

        String mappa = propmaps.getProperty("map");
        //mapcolors = propmaps.getProperty("color");
        touchevents = propevents.getProperty("touchevents");
        runevents = propevents.getProperty("runningevents");
        stepevents = propevents.getProperty("stepevents");
        steptile = (propmaps.getProperty("steptile")).charAt(0);

        //Checks which row of the Map is the longest, so to use it
        //to reference the Map's length
        for(i = 0; i < (mappa.split(" space")).length; i++)
        {
            if(mappa.split(" space")[i].length() > mappa.split(" space")[j].length())
            {
                j = i;
            }
        }

        mapx = mappa.split(" space")[j].length();
        mapy = (mappa.split(" space")).length;
        map = new char[mapx][mapy];

        //Saves in "map[][]" the Map where the Player is now
        for(j = 0; j < mapy; j++)
        {
            String mapLineX = mappa.split(" space")[j];

            for(i = 0; i < mapLineX.length(); i++)
            {
                map[i][j] = mapLineX.charAt(i);
            }
        }

        mapMovement(mapid, px, py, mappa);
    }

    //When called this function prints the Map on the Terminal
    public void drawMap(char[][] map, int mapx, int mapy)
    {
        int i = 0;
        int j = 0;

        System.out.print("\033[H\033[2J");  
        System.out.flush(); 

        do
        {
            do
            {   
                System.out.print(map[i][j]);
                i++;
            }
            while (i < mapx);
            i = 0;

            System.out.println("");
            j++;
        }
        while (j < mapy);

        System.out.print("\n    > ");
    }

    //This function is called when an IF command is read inside the Events File
    //For now, IF is only used to check if the global variables (var[]) or the local variables (mapvar[][]) result to a static value
    public void ifCondition(String event)
    {
        //If before the Variable ID we find a 'L', it means that it has to check
        //a Local Variable

        ifCheck = false;


        int varIndex = Integer.parseInt((event.split("\\(")[1]).split(" ")[1]);   //The Variable's Index
        String varOperation = ((event.split("\\(")[1]).split("\\)")[0]).split(" ")[2];    //The operation to commit on the Variable
        int varValue = Integer.parseInt(((event.split("\\(")[1]).split("\\)")[0]).split(" ")[3]);   //The value to which the Variable must be compared

        int varCondition = 0;   //This Var will take "var" o "mapvar"'s values to use them in the Check

        String typeIf = (event.split("\\(")[1]).split(" ")[0];
        switch(typeIf)
        {
            case "LVAR":
                varCondition = mapvar[varIndex][mapid];
                break;

            case "VAR":
                varCondition = var[varIndex];
                break;
        }

        if(typeIf.equals("LVAR") || typeIf.equals("VAR"))
        {
            //Here we check if it is required that the Variable is Bigger, Smaller or Equal to a specific value
            switch(varOperation)
            {
                case ">":
                    if(varCondition > varValue)
                    {
                        ifCheck = true;
                    }
                    break;

                case ">=":
                    if(varCondition >= varValue)
                    {
                        ifCheck = true;
                    }
                    break;

                case "<":
                    if(varCondition < varValue)
                    {
                        ifCheck = true;
                    }
                    break;

                case "<=":
                    if(varCondition <= varValue)
                    {
                        ifCheck = true;
                    }
                    break;

                case "==":
                    if(varCondition == varValue)
                    {
                        ifCheck = true;
                    }
                    break;

                case "!=":
                    if(varCondition != varValue)
                    {
                        ifCheck = true;
                    }
                    break;
            }
        }

        if(!ifCheck)
        {
            ifSkip = Integer.parseInt(event.split("\\) ")[1]); //Keeps track of the IF's, so to skip its contents
        }
    }

    //This function is called when the VAR or LVAR commands are found inside the Events File
    public void changeVar(String event)
    {
        //Keeps the Variable's index
        int varindex = Integer.parseInt((event.split("\\(")[1]).split(" ")[0]);

        //Keeps the Value with which the Variable must be changed
        int varChange = Integer.parseInt(((event.split("\\(")[1]).split("\\)")[0]).split(" ")[2]);

        //A value for which 'varChange' is multiplied
        int equals = 1;

        //Checks the kind of operation to use on the Variable
        switch((event.split("\\(")[1]).split(" ")[1])
        {
            //Sun
            case "+=":
                break;

            //Dif
            case "-=":
                varChange *= -1;
                break;

            //Equals
            case "=":
                equals = 0;
                break;
        }

        //Checks the type of the Variable
        switch(event.split("\\(")[0])
        {
            case "VAR":
                var[varindex] *= equals;
                var[varindex] += varChange;
                break;

            case "LVAR":
                mapvar[varindex][mapid] *= equals;
                mapvar[varindex][mapid] += varChange;                                                       
                break;
        }
    }

    //This function contains all the actions that can be made by Running, Step and Touch Events
    public void listEvents(String event)
    {
        String saveText = null;

        //If the conditions of an IF command are not respected, the events inside it are completely glossed over
        if(!ifCheck && (event.equals("ENDIF " + ifSkip) || event.equals("ELSE(" + ifSkip + ")") || event.equals("ENDELSE " + elseSkip)))
        {
            ifCheck = true;
        }

        if(ifCheck)
        {
            switch(event.split("\\(")[0])
            {
                //Common Events can be called on every map from the same Text File
                case "COMMONEVENT":
                    int commonEventID = Integer.parseInt((event.split("\\(")[1]).split("\\)")[0]);
                    String commonEvent = (propcommevents.getProperty("commonevents")).split(";")[commonEventID];

                    for(int i = 0; i < (commonEvent.split(" : ")).length; i++)
                    {
                        listEvents(commonEvent.split(" : ")[i]);
                    }
                    break;

                //Allows specific events to be executed only if a condition is met
                case "IF":
                    ifCondition(event);
                    break;

                //Allows specific events to be executed only if a condition is not met
                case "ELSE":
                    if(Integer.parseInt((event.split("\\(")[1]).split("\\)")[0]) != ifSkip)
                    {
                        ifCheck = false;
                        elseSkip = Integer.parseInt((event.split("\\(")[1]).split("\\)")[0]);
                    }
                    break;

                //Allows specific events to be repeated only if a condition is met
                case "WHEN":
                    ifCondition(event);
                    if(ifCheck)
                    {
                        loopCheck = true;
                        loopID = Integer.parseInt(event.split("\\) ")[1]);
                    }
                    else
                    {
                        ifCheck = true;
                    }
                    break;

                //Shows text 
                case "TEXT":
                    System.out.println("");
                    System.out.print("\n  "+ (event.split("\\(")[1]).split("\\)")[0]);
                    saveText = "\n  "+ (event.split("\\(")[1]).split("\\)")[0];
                    pause.nextLine();
                    break;

                //Changes the Tile in a specific part of the Map
                case "CHANGE":
                    int changex = Integer.parseInt((event.split("\\(")[1]).split("-")[0]);
                    int changey = Integer.parseInt(((event.split("\\(")[1]).split("-")[1]).split(" ")[0]);
                    char mapchange = ((event.split("\\(")[1]).split(" ")[1]).charAt(0);
                    map[changex][changey] = mapchange;
                    break;

                //Changes the Value of a Global or Local Variable
                case "VAR": case "LVAR":
                    changeVar(event);
                    break;

                //Refreshes the Terminal
                case "REFRESH":
                    runningEvents();
                    drawMap(map, mapx, mapy);
                    break;

                //Puts on hold
                case "WAIT":
                    int wait = Integer.parseInt((event.split("\\(")[1]).split("\\)")[0]);
                    try
                    {
                        Thread.sleep(wait);
                    }
                    catch(Exception ie)
                    {
                    }
                    break;

                //Gives the player a choice. Different events can be executed depending on the choice made
                //As of now is limited to only one Event per choice
                case "CHOICE":
                    boolean ripeti = true;
                    do
                    {
                        String evChoice = (event.split("CHOICE\\(")[1]);
                        String[] evChoices = new String[(evChoice.split("_")).length];

                        System.out.print("\n            ");
                        for(int countChoiches = 0; countChoiches < evChoices.length; countChoiches++)
                        {
                            evChoices[countChoiches] = (evChoice.split("_")[countChoiches]).split("\\[")[0];
                            System.out.print(evChoices[countChoiches]);

                            if((countChoiches+1) % 2 == 0)
                            {
                                System.out.print("\n    ");
                            }
                            else
                            {
                                System.out.print("    ");
                            }
                        }

                        System.out.print("\n                > ");
                        char scelta = choose.next().charAt(0);

                        String decisione = Character.toString(scelta);

                        try
                        {
                            Integer.parseInt(decisione);

                            if(Integer.parseInt(decisione)-1 > evChoices.length || Integer.parseInt(decisione)-1 < 0)
                            {
                                drawMap(map, mapx, mapy);
                                System.out.print("\n  " + saveText + "\n\n");
                            }
                            else
                            {
                                String sceltaFatta = ((evChoice.split("_")[Integer.parseInt(decisione)-1]).split("\\[")[1]).split("\\]")[0];
                                listEvents(sceltaFatta);
                                ripeti = false;
                            }
                        }
                        catch(Exception e)
                        {
                            drawMap(map, mapx, mapy);
                            System.out.print("\n  " + saveText + "\n\n");
                        }
                    }
                    while(ripeti);
                    break;

                //Moves the Player to another Map
                case "MAP":
                    int mapIDNew = Integer.parseInt(((event.split("\\)")[0]).split("\\(")[1]).split(" ")[0]);
                    int pxNew = Integer.parseInt((((event.split("\\)")[0]).split("\\(")[1]).split(" ")[1]).split("-")[0]);
                    int pyNew = Integer.parseInt((((event.split("\\)")[0]).split("\\(")[1]).split(" ")[1]).split("-")[1]);
                    changeMap(mapIDNew, pxNew, pyNew);
                    break;

                default:
                    break;
            }
        }
    }

    //This function controls "running events", events that are repeated at every refresh of the Terminal
    public void runningEvents()
    {
        if(runevents.length() == 1)
        {
        }
        else
        {
            for(int i = 0; i < (runevents.split(" : ")).length; i++)
            {
                String runevent = runevents.split(" : ")[i];
                listEvents(runevent);

                if(loopCheck)
                {
                    for(i = i; !(runevents.split(" : ")[i]).equals("LOOP " + loopID); i--)
                    {
                    }
                }
                loopCheck = false;
            }
        }
    }

    //This function controls "step events", events that are activated when the Player finds himself
    //at a specific coordinate
    public void stepEvents(int px, int py)
    {
        int i;
        int j;

        if(stepevents.length() == 1)
        {
        }
        else
        {
            String stepeventAction;

            for(i = 0; i < (stepevents.split(" /")).length; i++)
            {
                String stepeventPos = (stepevents.split(" /")[i].split(" : ")[0]);

                if(px == Integer.parseInt(stepeventPos.split("-")[0]) && py == Integer.parseInt(stepeventPos.split("-")[1]))
                {
                    for(j = 1; j < ((stepevents.split(" /")[i]).split(" : ")).length; j++)
                    {
                        stepeventAction = (stepevents.split(" /")[i].split(" : ")[j]);

                        listEvents(stepeventAction);

                        if(loopCheck)
                        {
                            for(j = j; !(stepevents.split(" /")[i].split(" : ")[j]).equals("LOOP " + loopID); j--)
                            {
                            }
                        }
                        loopCheck = false;
                    }
                    break;
                }
            }
        }
    }

    //This function controls "touch events", events that are activated when the player interacts (presses 'f')
    //with an adiacent coordinate
    public void touchEvents(String[] posizione, int mapx, int mapy)
    {
        int i;
        int j;

        String event = null;

        for(i = 0; i < (touchevents.split("/")).length; i++)
        {
            event = touchevents.split("/")[i];

            for(j = 0; j < 4; j++)
            {
                if(event.split(" : ")[0].equals(posizione[j]))
                {
                    for(int k = 1; k < (event.split(" : ")).length; k++)
                    {
                        listEvents(event.split(" : ")[k]);

                        if(loopCheck)
                        {
                            for(k = k; !(event.split(" : ")[k]).equals("LOOP " + loopID); k--)
                            {
                            }
                        }
                        loopCheck = false;
                    }
                    i = (touchevents.split("/")).length;
                    j = 4;
                }
            }
        }
    }

    public void mapMovement(int mapid, int px, int py, String mappa)
    {
        System.out.print("\033[H\033[2J");  
        System.out.flush(); 

    	char movimento = 'f';

        do
        {
            ifCheck = true;
            runningEvents();

            //Moves the player's icon at his coordinates
            map[px][py] = playericon;

            drawMap(map, mapx, mapy);

            ifCheck = true;
            stepEvents(px, py);

            String[] posizione = new String[]{(px+1)+"-"+py, (px-1)+"-"+py, px+"-"+(py+1), px+"-"+(py-1)};

            int xPlus = 0; int yPlus = 0;
            movimento = (movement.next()).charAt(0);

            switch(movimento)
            {
                case 'w': case 'W':
                    yPlus = -1;
                    break;

                case 'a': case 'A':
                    xPlus = -1;
                    break;

                case 's': case 'S':
                    yPlus = 1;
                    break;

                case 'd': case 'D':
                    xPlus = 1;
                    break;

                case 'f': case 'F':
                    ifCheck = true;
                    touchEvents(posizione, mapx, mapy);
                    break;

                default:
                    break;
            }

            int pxpy[] = tilesEffect(px, py, xPlus, yPlus);
            px = pxpy[0];
            py = pxpy[1];
        }
        while(true);
	}

    //This function is called whenever the player has changed position in the Map
    //Check the properties of the Tile the Player is going to walk on
    public int[] tilesEffect(int px, int py, int xPlus, int yPlus)
    {
        int i;
        int j;

        String tileffect = "WALK";

        boolean repeat = false;

        do
        {
            for(i = 0; i < (tiles.split(" ;")).length; i++)
            {
                if((tiles.split(" ;")[i]).charAt(0) == map[px+xPlus][py+yPlus])
                {
                    tileffect = (tiles.split(" ;")[i]).split(" ")[1];
                    break;
                }
            }

            switch(tileffect)
            {
                //This tile cannot be stepped on by the Player
                case "STOP":
                    repeat = false;
                    break;

                //This tile can be walked on
                case "WALK":
                    map[px][py] = steptile;
                    steptile = map[px+xPlus][py+yPlus];
                    playericon = '$';
                    px += xPlus; py += yPlus;
                    repeat = false;
                    break;

                //The Player walks "under" the tile
                case "THROUGH":
                    map[px][py] = steptile;
                    steptile = map[px+xPlus][py+yPlus];
                    playericon = steptile;
                    px += xPlus; py += yPlus;
                    repeat = false;
                    break;

                //The player repeats his last movement
                case "SLIP":
                    map[px][py] = steptile;
                    steptile = map[px+xPlus][py+yPlus];
                    playericon = '$';
                    px += xPlus; py += yPlus;
                    repeat = true;
                    map[px][py] = playericon;
                    drawMap(map, mapx, mapy);
                    try
                    {
                        Thread.sleep(250);
                    }
                    catch(InterruptedException ie)
                    {
                    }
                    break;
            }
        }
        while(repeat);

        return new int[]{px, py};
    }
}