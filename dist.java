/*press arrow key to move, press space key to shoot*/
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JFrame;
import javax.swing.JPanel;

/******* game graphics ********/
public class VehicleTrackerGraphDemo {
    //for the interface of the game
    public static void main(String[] args) {
        JFrame mainFrame = new JFrame("GUI");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setBounds(10, 10, 300, 300);
        mainFrame.setLayout(new BorderLayout());
        
        Map<String, MutablePoint> locations = new HashMap<>();
        for(int i=0; i<5; i++) {
            MutablePoint p = new MutablePoint();
            p.x = i* GameBoard.TANKWIDTH * 2;
            p.y = i* GameBoard.TANKHEIGHT * 2;
            locations.put("tank"+i, p);
        }
        
        MonitorVehicleTracker mv = new MonitorVehicleTracker(locations);
        
        final JPanel paintPanel = new GameBoard(mv);
        mainFrame.add(paintPanel, BorderLayout.CENTER);
        mainFrame.setVisible(true);
        
        Thread updater = new Thread(new Runnable(){

            @Override
            public void run() {
                while(!Thread.currentThread().isInterrupted()) {
                    try {
                        Thread.sleep(70);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    for(String id: mv.getLocations().keySet()) {
                        MutablePoint p = mv.getLocation(id);
                        if(p.alive) 
                            mv.setLocation(id, p.x + GameBoard.MOVESPEED, p.y + GameBoard.MOVESPEED * 2, true);
                    }
                    //System.out.println(javax.swing.SwingUtilities.isEventDispatchThread());
                    paintPanel.repaint();
                }
                
                
            }});
        updater.start();
    }
}

class GameBoard extends JPanel implements KeyListener {
    public static final int HEIGHT = 300;
    public static final int WIDTH = 400;
    public static final int TANKWIDTH = 30;
    public static final int TANKHEIGHT = 10;
    public static final int MOVESPEED = 2;
    public static final int TARGETX = 100;
    public static final int TARGETY = 200;
    private int dx = WIDTH / 2;
    private int dy = HEIGHT;
    private boolean enmeyAlive = true;
    private Rectangle2D yourtank = new Rectangle2D.Double(dx, dy, TANKWIDTH,
            TANKHEIGHT);
    //{System.out.println(javax.swing.SwingUtilities.isEventDispatchThread());}
    private Rectangle2D laserBeam = new Rectangle2D.Double(-10, -10, 0,
            0);
    private MonitorVehicleTracker mv;

    public GameBoard(MonitorVehicleTracker mv) {
        this.addKeyListener(this);
        this.setBackground(Color.white);
        this.setFocusable(true);
        this.mv = mv;
    }

    @Override
    protected void paintComponent(Graphics grphcs) {
        cleanDead();
        super.paintComponent(grphcs);
        Graphics2D gr = (Graphics2D) grphcs;
        gr.draw(yourtank);
        gr.draw(laserBeam);
        for(MutablePoint p: mv.getLocations().values()) {
            if (p.alive)
                drawTank(grphcs, p.x, p.y);
        }
        
    }

    private void cleanDead() {
        
        for(String id: mv.getLocations().keySet()) {
            MutablePoint p = mv.getLocation(id);
            if (overlaps(p.x, p.y, TANKWIDTH, TANKHEIGHT, laserBeam)) {
                enmeyAlive = false;
                mv.setLocation(id, -10, -10, false);
                
            }
            if (overlaps(p.x, p.y, TANKWIDTH, TANKHEIGHT, yourtank)) {
                this.removeKeyListener(this);
            }
        }
    }

    private boolean overlaps(int x, int y, int width, int height, Rectangle2D r) {
        return x < r.getX() + r.getWidth() && x + width > r.getX()
                && y < r.getY() + r.getHeight() && y + height > r.getY();
    }
    
    private void drawTank(Graphics g, int x, int y) {
        g.setColor(Color.yellow);
        g.draw3DRect(x, y, TANKWIDTH, TANKHEIGHT, true);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        System.out.println(e.getKeyCode());
        shoot();
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        moveRec(e);
        repaint();
    }

    @Override
    public void keyReleased(KeyEvent e) {
        laserBeam.setRect(-10, -10, 0, 0);  //hide it
        repaint();
    }

    public void shoot() {
        laserBeam.setRect(dx + TANKWIDTH/2, 0, 2, dy);
    }

    public void moveRec(KeyEvent evt) {
        switch (evt.getKeyCode()) {
        case KeyEvent.VK_LEFT:
            dx -= MOVESPEED;
            yourtank.setRect(dx, dy, TANKWIDTH, TANKHEIGHT);
            break;
        case KeyEvent.VK_RIGHT:
            dx += MOVESPEED;
            yourtank.setRect(dx, dy, TANKWIDTH, TANKHEIGHT);
            break;
        case KeyEvent.VK_UP:
            dy -= MOVESPEED;
            yourtank.setRect(dx, dy, TANKWIDTH, TANKHEIGHT);
            break;
        case KeyEvent.VK_DOWN:
            if (dy < HEIGHT)
                dy += MOVESPEED;
            yourtank.setRect(dx, dy, TANKWIDTH, TANKHEIGHT);
            break;
        }
    }

}

//for different locations
class MonitorVehicleTracker {
    private final Map<String, MutablePoint> locations;

    public MonitorVehicleTracker(Map<String, MutablePoint> locations) {
        this.locations = deepCopy(locations);
    }

    public synchronized Map<String, MutablePoint> getLocations() {
        return deepCopy(locations);
    }

    public synchronized MutablePoint getLocation(String id) {
        MutablePoint loc = locations.get(id);
        return loc == null ? null : new MutablePoint(loc);
    }

    public synchronized void setLocation(String id, int x, int y, boolean alive) {
        MutablePoint loc = locations.get(id);
        if (loc == null)
            throw new IllegalArgumentException("No such ID: " + id);
        loc.x = x;
        loc.y = y;
        loc.alive = alive;
    }

    private static Map<String, MutablePoint> deepCopy(Map<String, MutablePoint> m) {
        Map<String, MutablePoint> result = new HashMap<String, MutablePoint>();

        for (String id : m.keySet())
            result.put(id, new MutablePoint(m.get(id)));

        return Collections.unmodifiableMap(result);
    }
}


class MutablePoint {
    public int x, y;
    public boolean alive;

    public MutablePoint() {
        x = 0;
        y = 0;
        alive = true;
    }

    public MutablePoint(MutablePoint p) {
        this.x = p.x;
        this.y = p.y;
        this.alive = p.alive;
    }

}