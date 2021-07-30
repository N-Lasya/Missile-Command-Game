import java.awt.event.*;
import java.awt.*;
import java.applet.*;
import java.util.Vector;


public class MissileCommand extends Applet implements Runnable, MouseListener, MouseMotionListener{
	Vector Objects = new Vector();
	int cursorX, cursorY; // mouse location
	Thread thread;
	double frequency = 0.01; // chance of a meteor appearing in percent, each time delay
	int delay = 10; // time delay
	
	public static void main(String args[]) {
		Frame f = new Frame("Missile Command play");		
		f.addWindowListener(new WindowAdapter() {		
			public void windowClosing(WindowEvent e) {	
				System.exit(0);
			}
		});
		MissileCommand play = new MissileCommand();		
		f.add(play);				
		f.setSize(500, 500);			
		play.init(); 				
		play.start();				 
		f.setVisible(true); 			
	}
	public void init() {
		setBackground(Color.black);		//to set background
		setForeground(Color.white);		//to set foreground
		for (int n = 0; n < 3; n++)		//to create 3 missile MissileLaunchers on city
			Objects.addElement(new MissileLauncher(10 + 90*n, 190));
		for (int n = 0; n < 6; n++)		//to create city.
			Objects.addElement(new City(25 + 30*n, 199));		//there are 6 oval shaped objects placed which represents city
		addMouseListener(this);
		addMouseMotionListener(this);
		setCursor(new Cursor(java.awt.Cursor.CROSSHAIR_CURSOR));	//for crosshair type cursor
	}
	public void destroy() {	
		removeMouseListener(this);
		removeMouseMotionListener(this);
	}
	public void start() {			
		thread = new Thread(this);	
		thread.start();
		requestFocus();
	}
	public void stop() {			
		thread = null;				
	}
	
	Image offscreen = null;
	boolean firstTime = true;
	public void paint(Graphics g) {
		if (firstTime) {
			offscreen = createImage(getSize().width, getSize().height);
			firstTime = false;
		}
		Graphics og = offscreen.getGraphics();
		og.setColor(Color.black);
		og.clearRect(0, 0, getSize().width, getSize().height);	//clears the specified rectangle by filling it with the background colour of the current drawing surface.
		paintGraphics(og);
		g.drawImage(offscreen, 0, 0, this);
		og.dispose();
	}
	public void update(Graphics g) {
		paint(g);
	}
	void paintGraphics(Graphics g) {		// Paints each Object to the double buffer.
		for (int i = 0; i < Objects.size(); i++) {
			Object Object = (Object)Objects.elementAt(i);
			Object.draw(g);
			if (!Object.alive) {		// If the Object is not alive it is is removed from the Objects Vector. The Object is possibly replaced by another Object.
				Objects.removeElementAt(i--); // i is reduced by one because the index order is reduced by one
				Object newObject = Object.newObject();
				if (newObject != null)
					Objects.addElement(newObject);
			}
		}
	}

	public void run() {
		Thread me = Thread.currentThread();
		while (thread == me) {
			enemyMissileLaunch(); 	
			reach(); 	
			repaint();
			try {Thread.sleep(delay);} catch (InterruptedException e) {} // time delay
		}
	}
	void enemyMissileLaunch() {
		if (Math.random() > 1 - frequency)	
			Objects.addElement(new EnemyMissile());
	}
	void reach() {		// Traverses the Objects Vector and moves each Object
		for (int i = 0; i < Objects.size(); i++) {
			Object Object = (Object)Objects.elementAt(i);
			Object.move(Objects);
		}
	}
	public void mouseDragged(MouseEvent e) {}
	public void mouseMoved(MouseEvent e) {
		cursorX = e.getX();
		cursorY = e.getY();
	}
	public void mousePressed(MouseEvent e) {
		cursorX = e.getX();
		cursorY = e.getY();
		int smallestDistance = 1000;		// Calculates still usable MissileLauncher with smallest distance to target
		int nearestMissileLauncher = 1;
		for (int n = 0; n < 3; n++) {		//to iterate among 3 missile MissileLaunchers and return the missile MissileLauncher which is at minimal distance.
			int distance = (int)Math.sqrt((cursorX - ((MissileLauncher)Objects.elementAt(n)).xPosition)*(cursorX - ((MissileLauncher)Objects.elementAt(n)).xPosition) + (cursorY - ((MissileLauncher)Objects.elementAt(n)).yPosition)*(cursorY - ((MissileLauncher)Objects.elementAt(n)).yPosition));	//to calculate distance between missle MissileLauncher and mouse position.
			if (distance < smallestDistance && ((MissileLauncher)Objects.elementAt(n)).numberOfMissiles > 0) {
				smallestDistance = distance;
				nearestMissileLauncher = n;
			}
		}
		Missile missile;
		if ((missile = ((MissileLauncher)Objects.elementAt(nearestMissileLauncher)).launch((double)e.getX(), (double)e.getY())) != null)
			Objects.addElement(missile);
		repaint();
	}
	public void mouseReleased(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
 	public void mouseExited(MouseEvent e) {}
	public void mouseClicked(MouseEvent e) {}
}
	
class Object {		// Object is a superclass of all classes that have instances that appear on screen.
	boolean alive = true;		// alive is a flag that, when false, indicates that the Object is to be removed from the Objects Vector in the main class, next time there is a paint.
	double xPosition, yPosition;
	Object(double xPosition, double yPosition) {
		this.xPosition = xPosition;
		this.yPosition = yPosition;
	}
	void move(Vector Objects) {}		// Not all Objects use this method. Some use although they do not move.
	void draw(Graphics g) {}
	Object newObject() {		// Only some kinds of Object return a new object when they are removed.
		return null;
	}
	boolean inExplosion(Vector Objects) {		// Used to check if the Object is to close to one of the Explosions.
		for (int i = 0; i < Objects.size(); i++) {
			Object Object = (Object)Objects.elementAt(i);
			if (Object.within(xPosition, yPosition))
				return true;
		}
		return false;
	}
	boolean within(double x, double y) {		// Overridden only by Explosions.
		return false;
	}
}

class Missile extends Object {
	double xOrigin, yOrigin;
	double xDestination, yDestination;
	double xVelocity, yVelocity;
	Missile(double xPosition, double yPosition, double scalarVelocity, double xDestination, double yDestination) {
		super(xPosition, yPosition);
		xOrigin = xPosition;
		yOrigin = yPosition;
		this.xDestination = xDestination;
		this.yDestination = yDestination;
		double hyp = Math.sqrt((xOrigin - xDestination)*(xOrigin - xDestination) + (yOrigin - yDestination)*(yOrigin - yDestination));		// Velocity is separated in x and y and calculated by using a scalar speed value and the travel distance.
		xVelocity = scalarVelocity/hyp*(xDestination - xOrigin);
		yVelocity = scalarVelocity/hyp*(yDestination - yOrigin);
	}
	boolean atDestination() {		// Used to check if the missile has reached its destination.
		if (Math.abs(xPosition - xDestination) < 2.1 && Math.abs(yPosition - yDestination) < 2.1)
			return true;
		return false;
	}
	void move(Vector Objects) {		// Moves the missile along its path and checks if it has reached its destination or is to close to an Explosion.
		xPosition += xVelocity;
		yPosition += yVelocity;
		if (atDestination() || inExplosion(Objects))
			alive = false;
	}
	void draw(Graphics g) {}
	Object newObject() {		
		return new Explosion(xPosition, yPosition);
	}
}

class EnemyMissile extends Missile {		//for meteors 
	EnemyMissile() {
		super(200*Math.random(), 0, 0.3*Math.random()+0.3, 200*Math.random(), 200);
	}
	void draw(Graphics g) {
		g.setColor(Color.red);
		g.drawLine((int)xOrigin, (int)yOrigin, (int)(xPosition), (int)(yPosition));
		g.setColor(Color.white);
		g.drawLine((int)(xPosition), (int)(yPosition), (int)(xPosition), (int)(yPosition));
	}
}

class PatriotMissile extends Missile {		//for missiles
	PatriotMissile(double xPosition, double yPosition, double xDestination, double yDestination) {		// An x marks the destination of a PatriotMissile missile.
		super(xPosition, yPosition, 3.0, xDestination, yDestination);
	}
	void draw(Graphics g) {
		g.setColor(Color.magenta);
		g.drawLine((int)xOrigin, (int)yOrigin, (int)(xPosition), (int)(yPosition));
		g.setColor(Color.white);
		g.drawLine((int)xDestination-2, (int)yDestination-2, (int)xDestination+2, (int)yDestination+2);
		g.drawLine((int)xDestination-2, (int)yDestination+2, (int)xDestination+2, (int)yDestination-2);
	}
}

class MissileLauncher extends Object {		// MissileLaunchers are invulnerable to meteor and have a number of missiles (numberOfMissiles). Until the MissileLaunchers supply of missiles are expended the MissileLauncher may launch PatriotMissile missiles.
	int numberOfMissiles = 10;
	MissileLauncher(double xPosition, double yPosition) {
		super(xPosition, yPosition);
	}
	PatriotMissile launch(double xDestination, double yDestination) {		// Creates a PatriotMissile missile and launches it towards its destination. If there are any missiles left.
		if (numberOfMissiles-- > 0)
			return new PatriotMissile(xPosition, yPosition, xDestination, yDestination);
		return null;
	}
	void draw(Graphics g) {
		g.setColor(Color.white);
		int [] x = {(int)xPosition-4, (int)xPosition, (int)xPosition+4};
		int [] y = {(int)yPosition+12, (int)yPosition, (int)yPosition+12};
		if (numberOfMissiles > 4)
			g.fillPolygon(x, y, 3);
		else if (numberOfMissiles > 0)
			g.drawPolygon(x, y, 3);
	}
}

class City extends Object {		// A City is vulnerable to explosions. 
	City(double xPosition, double yPosition) {		//it represents the city when Destruction.
		super(xPosition, yPosition);
	}
	void move(Vector Objects) {
		if (inExplosion(Objects))
			alive = false;
	}
	void draw(Graphics g) {
		g.setColor(Color.lightGray);
		g.fillOval((int)xPosition - 6, (int)yPosition - 3, 12, 6);
	}
	Object newObject() {
		return new Destruction(xPosition, yPosition);
	}
}

class Explosion extends Object {		// Explosions are Objects that expand and shrink.
	double size = 0;
	boolean expanding = true;
	Explosion(double xPosition, double yPosition) {
		super(xPosition, yPosition);
	}
	void move(Vector Objects) {		// Expand the explosion until it has reached its final size, then shrink until zero, and then set it for deletion.
		if (expanding) {		//expanding
			if (size < 10)
				size += 0.1;
			else
				expanding = false;
		}
		else {					//shrinking
			if (size > 0)
				size -= 0.1;
			else
				alive = false;
		}
	}
	void draw(Graphics g) {		// Explosions has one of three random colors each time it is painted. 
		g.setXORMode(Color.black);		//Explosions are drawn in XORMode
		double r = Math.random();
		if (r < 0.3)
			g.setColor(Color.magenta);
		else if (r < 0.6)
			g.setColor(Color.orange);
		else
			g.setColor(Color.white);
		g.fillOval((int)(xPosition - size), (int)(yPosition - size), (int)size*2, (int)size*2);
		g.setPaintMode();
	}
	boolean within(double x, double y) {		// Used to check whether a position is inside the Explosion (to close).
		if (Math.abs(x - xPosition) < size && Math.abs(y - yPosition) < size)	//if in explosion the method is overriden to return true.
			return true;
		return false;
	}
}

class Destruction extends Object {		// A Destruction is whats left of a City when it is removed.
	Destruction(double xPosition, double yPosition) {
		super(xPosition, yPosition);
	}
	void draw(Graphics g) {
		g.setColor(Color.white);
		g.drawOval((int)xPosition - 6, (int)yPosition - 3, 12, 6);
	}
}

