package com.ruthless.less.boredom;

import com.ruthless.less.database.AppDatabase;
import com.ruthless.less.database.dao.BoredomMessageHistoryDao;
import com.ruthless.less.database.entities.BoredomMessageHistoryEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Boredom / chiding message pools. Mildly negative — never abusive.
 * Home and drawer use separate, context-specific pools.
 */
public final class BoredomMessageManager {

    public interface MessageCallback {
        void onMessage(String message);
    }

    /** Home screen: unlock, idle stare, empty home, pocket check. */
    private static final String[] HOME_MESSAGES = {
            "WELCOME BACK.\nYOU PROBABLY\nDIDN'T NEED TO\nCHECK YOUR PHONE.",
            "YOU ARE BACK.\nNOTHING IMPORTANT\nHAPPENED WHILE YOU\nWERE AWAY.",
            "THE PHONE IS STILL\nHERE.\n\nYOUR TIME IS NOT.",
            "YOU COULD BE DOING\nSOMETHING ELSE.",
            "THERE IS NOTHING\nINTERESTING HERE.",
            "WHY ARE YOU HERE?",
            "THIS IS A PHONE.\n\nNOT A PLACE TO LIVE.",
            "YOU HAVE CHECKED\nYOUR PHONE MANY TIMES\nTODAY.\n\nIT HAS NOT BECOME\nMORE INTERESTING.",
            "PUT IT DOWN.",
            "YOUR PHONE HAS\nNOTHING NEW TO OFFER.",
            "YOU ARE BORED.\n\nTHE PHONE WILL NOT\nFIX THAT.",
            "THAT WAS UNNECESSARY.",
            "YOU WERE DOING FINE\nWITHOUT YOUR PHONE.",
            "NOTHING HERE NEEDS\nYOUR ATTENTION.",
            "THIS COULD HAVE\nWAITED.",
            "YOU DON'T NEED TO\nCHECK AGAIN.",
            "STILL HERE?",
            "NO EMERGENCY.",
            "YOUR PHONE IS NOT\nGOING ANYWHERE.",
            "THAT WAS A VERY\nPRODUCTIVE UNLOCK.",
            "WHY?",
            "THE SCREEN IS ON.\nYOUR ATTENTION IS OFF.",
            "LESS IS DOING ITS JOB.\nARE YOU?",
            "YOU ALREADY KNOW\nWHAT IS HERE.",
            "ANOTHER LOOK.\nSAME PHONE.",
            "TIME PASSES EITHER WAY.",
            "THIS HABIT IS OPTIONAL.",
            "NOTHING CHANGED\nSINCE LAST TIME.",
            "YOUR POCKET WAS FINE.",
            "LEAVE IT.\nWALK AWAY.",
            "THE WORLD OUTSIDE\nSTILL EXISTS.",
            "BOREDOM IS ALLOWED.",
            "YOU ARE NOT MISSING\nANYTHING IMPORTANT.",
            "CHECKING AGAIN\nCHANGES NOTHING.",
            "SIT WITH THE QUIET.",
            "THE NOTIFICATIONS\nCAN WAIT.",
            "ONE MORE LOOK\nWILL NOT HELP.",
            "PUT THE PHONE FACE DOWN.",
            "YOU CAME BACK FOR\nNO GOOD REASON.",
            "THIS IS THE BORING PART.\nTHAT IS THE POINT.",
            "ATTENTION IS EXPENSIVE.",
            "YOU CAN STOP NOW.",
            "GO DO THE THING\nYOU MEANT TO DO.",
            "UNLOCKED.\nUNINSPIRED.",
            "LESS NOISE.\nMORE LIFE.",
            "YOUR FUTURE SELF\nWANTED LESS OF THIS.",
            "NO NEW MEANING HERE.",
            "CLOSE IT.\nKEEP MOVING.",
            "THE DEFAULT IS TO STAY.\nCHOOSE TO LEAVE.",
            "ENOUGH.",
            "LOOKING AGAIN\nWILL NOT HELP.",
            "YOU OPENED THIS\nOUT OF HABIT.",
            "HABIT IS NOT NEED.",
            "THERE IS NO PRIZE\nFOR CHECKING.",
            "YOUR TIME IS LEAKING.",
            "PUT IT AWAY.",
            "THE PHONE CAN WAIT.",
            "YOU ALREADY CHECKED.",
            "WALK FIRST.\nPHONE LATER.",
            "THIS SCREEN IS EMPTY\nON PURPOSE.",
            "BOREDOM IS NOT\nAN EMERGENCY.",
            "YOU ARE FILLING TIME.\nNOT USING IT.",
            "STOP HUNTING\nFOR STIMULATION.",
            "THE QUIET IS FINE.",
            "NO ONE IS WAITING\nON THIS SCREEN.",
            "YOUR HANDS KNOW\nTHIS PATH TOO WELL.",
            "BREAK THE LOOP.",
            "THIS IS NOT REST.",
            "YOU CAN BE DONE NOW.",
            "THERE IS LIFE\nOFFSCREEN.",
            "THE URGE WILL PASS.",
            "KEEP YOUR EVENING.",
            "KEEP YOUR MORNING.",
            "DO NOT FEED THE HABIT.",
            "LESS OPENING.\nMORE LIVING.",
            "HOME IS EMPTY.\nTHAT IS THE FEATURE.",
            "THE CLOCK DID NOT\nNEED YOU.",
            "YOU UNLOCKED FOR\nNOTHING.",
            "THIS HOME SCREEN\nHAS NO TREATS.",
            "STARE AT THE CLOCK\nOR PUT IT DOWN.",
            "POCKET CHECK COMPLETE.\nNOW STOP.",
            "THE HOME SCREEN\nWILL NOT ENTERTAIN YOU.",
            "YOU ARE LOOKING AT\nBLACK AND WHITE.\nLEAVE.",
            "NO ICONS.\nNO BAIT.\nSTILL HERE?",
            "UNLOCK WAS THE MISTAKE.\nLOCK IS THE FIX.",
            "THE TIME IS RIGHT THERE.\nYOU DID NOT NEED MORE.",
            "EMPTY SPACE IS\nNOT A PROBLEM.",
            "YOU ARE HOME.\nNOW LEAVE THE PHONE.",
            "THIS IS NOT A FEED.",
            "NO STORIES HERE.\nGOOD.",
            "THE BATTERY IS FINE.\nYOU ARE NOT.",
            "ANOTHER UNLOCK.\nSAME EMPTINESS.",
            "YOU CHECKED THE TIME.\nYOU CAN GO.",
            "THE HOME SCREEN\nOWES YOU NOTHING.",
            "WAITING FOR SOMETHING\nTHAT WILL NOT APPEAR.",
            "THIS IS A WALL.\nNOT A WINDOW.",
            "PUT IT IN YOUR POCKET.\nKEEP IT THERE.",
            "YOU ARE STANDING STILL\nWITH A LIT SCREEN.",
            "THE DAY IS ELSEWHERE.",
            "NO BADGE.\nNO PING.\nSTILL UNLOCKED.",
            "YOU CAME TO THE HOME\nWITH NO PLAN.",
            "PLANS DO NOT LIVE\nON THIS SCREEN.",
            "THE CLOCK TICKS\nWITHOUT YOU.",
            "LOOK UP.\nNOT DOWN.",
            "THIS SILENCE IS\nINTENTIONAL.",
            "YOU ARE BETWEEN\nREAL THINGS.",
            "DO NOT START\nA SESSION FROM HERE.",
            "HOME MEANS DONE.\nNOT BEGIN.",
            "THE PHONE IS A TOOL.\nYOU ARE USING IT\nAS A HABIT.",
            "BLACK SCREEN ENERGY.\nKEEP IT.",
            "YOU DID NOT MISS\nA MESSAGE WORTH THIS.",
            "THE ROOM IS STILL THERE\nWHEN YOU LOOK AWAY.",
            "STOP REFRESHING\nA STATIC HOME.",
            "LESS HOME.\nMORE AWAY.",
            "YOU ARE SAFE.\nTHE PHONE CAN SLEEP.",
            "IDLE IS FINE.\nUNLOCK IS OPTIONAL.",
            "THE HOME SCREEN\nIS A DEAD END.\nTURN AROUND.",
            "YOU BROUGHT THE URGE.\nLEAVE IT HERE.",
            "NO APP IS CALLING YOU\nFROM THIS SCREEN.",
            "THIS IS THE CALM.\nDO NOT RUIN IT.",
            "YOU ALREADY KNOW\nTHE TIME.",
            "CHECKING HOME\nIS STILL CHECKING.",
            "THE EMPTY HOME\nIS WORKING.",
            "DO NOT SWIPE UP\nJUST TO FEEL BUSY.",
            "STAY ON HOME.\nOR LOCK.\nNOT THE DRAWER.",
            "YOUR ATTENTION\nARRIVED WITH NO JOB.",
            "SEND IT BACK.",
            "THE WORLD DID NOT\nPAUSE FOR THIS UNLOCK.",
            "YOU CAN RELIVE\nTHIS MOMENT LATER\nBY NOT OPENING ANYTHING.",
            "HOME.\nTHEN OFF.",
            "THIS IS THE LAST STOP\nBEFORE WASTING TIME.",
            "GET OFF AT HOME.",
            "THE PHONE IS COLD.\nKEEP IT THAT WAY.",
            "YOU ARE NOT LATE\nFOR ANYTHING IN HERE.",
            "NO COUNTDOWN.\nNO FEED.\nGO.",
            "THE HOME SCREEN\nHAS SAID ALL IT WILL SAY.",
            "BLANK SPACE\nIS A KIND OF ANSWER.",
            "YOU UNLOCKED.\nTHAT IS ENOUGH ACTION\nFOR NOW.",
            "LET THE SCREEN\nGO DARK.",
            "YOUR NEXT GOOD CHOICE\nIS TO LOCK IT.",
            "DO NOT FILL THE QUIET\nWITH AN APP.",
            "THE CLOCK IS HONEST.\nTHE URGE IS NOT.",
            "HOME IS FOR LEAVING.\nNOT LINGERING.",
            "YOU ARE DONE\nBEFORE YOU START.",
            "PROTECT THE EMPTY.",
            "THIS SCREEN REFUSES\nTO ENTERTAIN YOU.\nRESPECT THAT.",
            "LESS CHECKING.\nMORE BEING.",
            "THE HABIT WANTS\nONE MORE LOOK.\nDENY IT.",
            "YOU ARE ALREADY\nWHERE YOU NEED TO BE.\nOFF THE PHONE.",
            "LOCK IT.\nWALK.",
            "THE HOME SCREEN\nIS NOT HIDING A SURPRISE.",
            "SURPRISE: THERE IS NONE.",
            "YOU CAME FOR A HIT\nOF NOTHING.",
            "NOTHING DELIVERED.\nSESSION OVER.",
            "THANK THE EMPTINESS.\nTHEN LEAVE.",
            "THIS IS YOUR EXIT.",
            "TAKE IT."
    };

    /** App drawer: browsing lists, hunting apps, habit scrolling. */
    private static final String[] DRAWER_MESSAGES = {
            "THE APP DRAWER\nIS NOT A DESTINATION.",
            "SCROLLING WILL NOT\nMAKE THIS BETTER.",
            "THE LIST IS NOT\nENTERTAINMENT.",
            "LEAVE THE DRAWER.\nLEAVE THE PHONE.",
            "SAME APPS.\nSAME URGE.",
            "YOU ARE BROWSING\nYOUR OWN HABITS.",
            "LOOKING FOR SOMETHING\nTO WASTE TIME ON.",
            "THE LIST DID NOT CHANGE\nSINCE YESTERDAY.",
            "PICK ONE.\nOR PICK NONE.",
            "NONE IS AVAILABLE.",
            "YOU OPENED THE DRAWER\nWITH NO ERRAND.",
            "ERRANDLESS SCROLLING.",
            "THIS IS A MENU.\nNOT A FEED.",
            "STOP WINDOW SHOPPING\nYOUR APPS.",
            "EVERY NAME HERE\nWANTS YOUR TIME.",
            "NONE OF THEM\nNEED IT RIGHT NOW.",
            "YOU ARE HUNTING.\nTHERE IS NO PREY.",
            "THE DRAWER IS A TRAP\nYOU BUILT YOURSELF.",
            "CLOSE THE LIST.\nKEEP YOUR DAY.",
            "BROWSING APPS\nIS STILL PHONE USE.",
            "YOU ALREADY KNOW\nWHAT IS INSTALLED.",
            "READING LABELS\nIS NOT PRODUCTIVITY.",
            "THE ALPHABET\nWILL NOT SAVE YOU.",
            "SWIPE UP WAS EASY.\nSWIPE DOWN IS WISER.",
            "GO HOME.\nOR GO AWAY.",
            "THIS LIST IS LONG\nON PURPOSE.\nLEAVE EARLY.",
            "YOU CAME TO FIND\nA DISTRACTION.",
            "YOU FOUND THE LIST.\nTHAT IS ENOUGH.",
            "DO NOT START\nAN APP JUST TO END\nTHE QUIET.",
            "THE QUIET WAS FINE.",
            "SCANNING NAMES\nFEEDS THE LOOP.",
            "STOP AT THE TOP.\nLEAVE.",
            "MID-LIST IS WHERE\nBAD DECISIONS LIVE.",
            "YOU DO NOT NEED\nA NEW APP SESSION.",
            "THE APP YOU WANT\nWANTS YOU MORE.",
            "THAT IS THE PROBLEM.",
            "DRAWER OPEN.\nATTENTION OPEN.\nCLOSE BOTH.",
            "YOU ARE BETWEEN APPS.\nSTAY BETWEEN.",
            "NO SELECTION\nIS A SELECTION.",
            "CHOOSE EXIT.",
            "THE LIST WILL BE HERE\nWHEN YOU HAVE A REASON.",
            "RIGHT NOW YOU DO NOT.",
            "HABIT OPENED THIS.\nCHOICE CAN CLOSE IT.",
            "YOU ARE NOT SEARCHING.\nYOU ARE STALLING.",
            "STALL ELSEWHERE.",
            "OFFSCREEN.",
            "PINNED OR NOT,\nTHE URGE IS THE SAME.",
            "DO NOT REWARD\nTHE SWIPE UP.",
            "THE DRAWER OWES YOU\nNO FUN.",
            "PLAIN TEXT.\nPLAIN TRUTH.\nLEAVE.",
            "YOU ARE READING APPS\nLIKE A MAGAZINE.",
            "THIS IS NOT READING.\nTHIS IS DELAYING.",
            "PICK UP YOUR LIFE.\nPUT DOWN THE LIST.",
            "EVERY TAP HERE\nSTARTS A CLOCK.",
            "YOU CAN STILL\nSTART NOTHING.",
            "BEST APP:\nNONE.",
            "SECOND BEST:\nGO HOME.",
            "THIRD BEST:\nLOCK THE PHONE.",
            "YOU OPENED THIS\nTO FEEL BUSY.",
            "BUSY IS NOT USEFUL.",
            "THE NAMES ARE FAMILIAR.\nTHE WASTE IS TOO.",
            "YOU HAVE SEEN\nTHIS LIST BEFORE.",
            "IT DID NOT HELP THEN.",
            "IT WILL NOT HELP NOW.",
            "STOP MID-SCROLL.",
            "FREEZE.\nTHEN BACK OUT.",
            "THE APP YOU ALMOST OPEN\nCAN WAIT FOREVER.",
            "FOREVER IS FINE.",
            "YOU ARE IN THE AISLE.\nDO NOT BUY.",
            "WINDOW SHOPPING\nSTILL COSTS ATTENTION.",
            "ATTENTION SPENT HERE\nIS GONE.",
            "LEAVE WITH YOUR\nATTENTION INTACT.",
            "NO ICON.\nNO COLOR.\nSTILL TEMPTING?",
            "TEMPTATION DOES NOT\nNEED DECORATION.",
            "YOU MADE IT HARDER.\nNOW MAKE IT RARE.",
            "RARE MEANS CLOSED.",
            "THE DRAWER IS OPEN.\nTHAT WAS THE MISTAKE.",
            "UNDO IT.",
            "SCROLLING IS A TELL.\nYOU ARE UNSETTLED.",
            "SIT WITH THAT.\nOFF THE LIST.",
            "YOU WILL NOT FIND\nPEACE IN AN APP NAME.",
            "PEACE IS OUTSIDE\nTHIS PANEL.",
            "THIS IS THE BORING LIST.\nTREAT IT AS BORING.",
            "DO NOT TURN BOREDOM\nINTO A LAUNCH.",
            "LAUNCH IS THE FAILURE\nMODE.",
            "FAILURE MODE:\nAVOIDED, IF YOU LEAVE.",
            "ONE FINGER AWAY\nFROM WASTING AN HOUR.",
            "MOVE THAT FINGER\nTO BACK.",
            "THE LIST IS PATIENT.\nYOU SHOULD NOT BE.",
            "IMPATIENCE HERE\nLOOKS LIKE SCROLLING.",
            "IMPATIENCE ELSEWHERE\nLOOKS LIKE LIVING.",
            "CHOOSE LIVING.",
            "YOU OPENED THE DRAWER\nBECAUSE HOME WAS QUIET.",
            "QUIET WAS THE WIN.",
            "YOU ARE UNDOING IT.",
            "STOP UNDOING IT.",
            "CONFUSE ME OR NOT,\nTHE HABIT IS YOURS.",
            "OWN THE EXIT.",
            "THE APPS ARE NOT\nGOING ANYWHERE.",
            "YOU SHOULD.",
            "BROWSE LESS.\nDECIDE LESS.\nLEAVE MORE.",
            "DECISION FATIGUE\nSTARTS HERE.",
            "SKIP THE DECISION.\nSKIP THE SESSION.",
            "EMPTY HANDS.\nCLOSED DRAWER.",
            "THAT IS THE MOVE.",
            "YOU ARE NOT LATE\nFOR ANY APP.",
            "NO APP IS LATE\nFOR YOU.",
            "GOOD. KEEP IT THAT WAY.",
            "THE LIST ENDS.\nSO SHOULD THIS.",
            "END BEFORE THE END.",
            "EARLY EXIT\nIS THE POINT.",
            "YOU CAME LOOKING.\nFOUND NOTHING WORTH IT.",
            "ACCEPT THAT.\nGO.",
            "THE DRAWER IS A MIRROR.\nIT SHOWS THE URGE.",
            "LOOK AWAY.",
            "NO NEW APPS APPEARED.\nNO NEW REASONS EITHER.",
            "SAME GRID OF NAMES.\nSAME OLD LOOP.",
            "BREAK IT BY CLOSING.",
            "NOT BY OPENING.",
            "OPENING IS HOW\nTHE LOOP WINS.",
            "CLOSING IS HOW\nYOU WIN.",
            "WIN NOW.",
            "TEXT ONLY.\nTIME ONLY.\nLEAVE ONLY.",
            "THIS PANEL HAS\nNO REWARD.",
            "DO NOT INVENT ONE.",
            "YOU ARE DONE\nWITH THIS LIST.",
            "ACT LIKE IT.",
            "SWIPE DOWN.\nOR HOME.\nOR LOCK.",
            "ANYTHING BUT\nANOTHER APP.",
            "THE NEXT APP\nCAN WAIT A DAY.",
            "A DAY IS CHEAP.",
            "AN HOUR IN AN APP\nIS NOT.",
            "PROTECT THE HOUR.",
            "PROTECT THE AFTERNOON.",
            "PROTECT THE NIGHT.",
            "START BY CLOSING\nTHE DRAWER.",
            "THE LIST WILL FORGIVE YOU\nFOR LEAVING.",
            "YOUR FUTURE SELF\nWILL TOO.",
            "LESS DRAWER.\nMORE DONE.",
            "DONE LOOKS LIKE\nA CLOSED LIST.",
            "CLOSE IT.",
            "NOW."
    };

    /** Shown when opening a CONFUSE ME app. */
    private static final String[] CONFIRM_CHIDES = {
            "DO YOU ACTUALLY\nNEED THIS?",
            "THIS IS THE HABIT\nTALKING.",
            "YOU CAN STILL\nWALK AWAY.",
            "OPENING THIS WILL NOT\nFIX YOUR MOOD.",
            "ARE YOU SURE\nTHIS MATTERS RIGHT NOW?",
            "THIS APP IS WHY\nYOU MADE IT HARD.",
            "PAUSE.\nTHEN DECIDE.",
            "YOU HID THIS\nFOR A REASON.",
            "ONE TAP AWAY FROM\nWASTING MORE TIME.",
            "THE BORING CHOICE\nIS THE BETTER ONE.",
            "YOU DO NOT OWE\nTHIS APP YOUR ATTENTION.",
            "IF YOU ARE HESITATING,\nCHOOSE NO.",
            "THIS WILL STILL BE HERE\nLATER. YOU DO NOT NEED IT NOW.",
            "ANOTHER ROUND?\nREALLY?",
            "YOU CAME LOOKING\nFOR THIS. THAT IS THE PROBLEM.",
            "LEAVE IT CLOSED.",
            "YOUR FUTURE SELF\nIS WATCHING.",
            "THIS IS OPTIONAL.\nSKIP IT.",
            "YOU ALREADY KNOW\nHOW THIS ENDS.",
            "MAKE IT HARDER\nFOR THE HABIT."
    };

    /** Shown after declining a CONFUSE ME open. */
    private static final String[] DECLINE_CHIDES = {
            "PROBABLY BETTER.",
            "GOOD.",
            "THAT WAS THE RIGHT CALL.",
            "NICE SAVE.",
            "SEE? YOU CAN STOP.",
            "BETTER LEFT CLOSED.",
            "YOUR TIME THANKS YOU.",
            "HABIT INTERRUPTED.",
            "WELL DONE.",
            "KEEP GOING.",
            "THAT URGE CAN WAIT.",
            "CLOSED IS FINE.",
            "YOU DID NOT NEED IT.",
            "WALK AWAY CLEAN.",
            "LESS WINS THIS ROUND.",
            "STAY BORED.\nSTAY FREE.",
            "THAT WAS STRENGTH.",
            "BACK TO REAL LIFE.",
            "THE PHONE CAN SURVIVE\nWITHOUT THIS.",
            "GOOD CHOICE."
    };

    private final BoredomMessageHistoryDao dao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Random random = new Random();

    public BoredomMessageManager(AppDatabase database) {
        this.dao = database.boredomMessageHistoryDao();
    }

    public int poolSize() {
        return HOME_MESSAGES.length;
    }

    public static int homePoolSize() {
        return HOME_MESSAGES.length;
    }

    public static int drawerPoolSize() {
        return DRAWER_MESSAGES.length;
    }

    public static int confirmPoolSize() {
        return CONFIRM_CHIDES.length;
    }

    public static int declinePoolSize() {
        return DECLINE_CHIDES.length;
    }

    /** Home-screen chiding with recent-history avoidance. */
    public void nextMessageAsync(MessageCallback callback) {
        executor.execute(() -> {
            List<Integer> recent = dao.recentMessageIds(12);
            int pick = pickAvoiding(random, HOME_MESSAGES.length, recent);
            BoredomMessageHistoryEntity row = new BoredomMessageHistoryEntity();
            row.messageId = pick;
            row.displayedAt = System.currentTimeMillis();
            dao.insert(row);
            dao.pruneBefore(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000);
            callback.onMessage(HOME_MESSAGES[pick]);
        });
    }

    /** Drawer chiding — context-specific, no DB write. */
    public String nextDrawerChide() {
        return DRAWER_MESSAGES[random.nextInt(DRAWER_MESSAGES.length)];
    }

    public String nextConfirmChide() {
        return CONFIRM_CHIDES[random.nextInt(CONFIRM_CHIDES.length)];
    }

    public String nextDeclineChide() {
        return DECLINE_CHIDES[random.nextInt(DECLINE_CHIDES.length)];
    }

    /** Test helper: pick avoiding a forbidden set without DB. */
    public static int pickAvoiding(Random random, int poolSize, List<Integer> recent) {
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < poolSize; i++) {
            if (!recent.contains(i)) {
                candidates.add(i);
            }
        }
        if (candidates.isEmpty()) {
            return random.nextInt(poolSize);
        }
        return candidates.get(random.nextInt(candidates.size()));
    }
}
