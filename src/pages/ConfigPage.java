package pages;

import static client.Constant.BUTTON_HEIGHT;
import static client.Constant.BUTTON_WIDTH;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import system.Rule;
import client.Client;
import client.ClientOperator;
import client.MahjongFrame;

public class ConfigPage extends GraphicalPage implements MouseListener, Page{
	private Rule rule;
	private List<Config> configs;
	private boolean isFinish;
	private Image imgBuffer;
	private Graphics g2;

	private class PaintThread extends Thread{
		public void run(){
			while(!isFinish){
				repaint();
				try{
					sleep(10);
				}catch(InterruptedException e){}
			}
		}
	}
	
	private class Config{
		private State state;
		private boolean flag;
		public Config(State state){
			this.state = state;
			flag = false;
		}
		public void toggleFlag(){
			flag = !flag;
		}
		public String getExplain(){
			return state.explain;
		}
		public String getSwitch(){
			if(flag)
				return "ON";
			else
				return "OFF";
		}
	}
	
	private enum State{
		AKA("赤ドラ有り"),
		KOKUSI("国士13面待ちはダブル役満"),
		SUAN("四暗刻単騎待ちはダブル役満"),
		SUSI("大四喜はダブル役満"),
		KAZE("半荘戦まで行う"),
		SHANYU("西入する"),
		HAKO("とびで終了する");
		private String explain;
		private State(String exp){
			explain = exp;
		}
	}
	{
		configs = new ArrayList<Config>();
		for(State st:State.values()){
			configs.add(new Config(st));
		}
		isFinish = false;
	}
	
	public ConfigPage(MahjongFrame frame){
		setFrame(frame);
		addMouseListener(this);
		new PaintThread().start();
	}
	public ConfigPage(MahjongFrame frame,Client operator){
		this(frame);
		setOperator(operator);
		if(operator != null)
			((ClientOperator)getOperator()).setPage(this);
	}
	
	public void paint(Graphics g){
		if(imgBuffer == null)
			imgBuffer = createImage(getWidth(),getHeight());
		if(g2 == null)
			g2 = imgBuffer.getGraphics();
		super.paint(g2);
		g2.clearRect(0, 0, getWidth(), getHeight());
		int x = getWidth() * 2 / 5;
		int y = getHeight()/5;

		g2.setFont(new Font("",Font.BOLD,40));
		g2.drawString("C O N F I G", x, y - BUTTON_HEIGHT * 3 / 2);
		
		int maxLength = 0;
		for(Config c:configs){
			int tmp = c.getExplain().length();
			if(tmp > maxLength)
				maxLength = tmp;
		}
		for(Config c:configs){
			g2.setFont(new Font("",Font.BOLD,20));
			if(c.flag)
				g2.setColor(Color.RED);
			else
				g2.setColor(Color.BLUE);
			g2.fillRect(x + maxLength * 22 + 14, y, BUTTON_WIDTH, BUTTON_HEIGHT);
			g2.setColor(Color.BLACK);
			g2.drawString(c.getSwitch(), x + maxLength * 22 + 45, y + BUTTON_HEIGHT/2 + 7);
			g2.setColor(Color.BLACK);
			g2.drawString(c.getExplain(), x, y + BUTTON_HEIGHT/2 + 7);
			y += BUTTON_HEIGHT + 20;
		}
		g2.setColor(Color.GRAY);
		g2.fillRect((getWidth() - BUTTON_WIDTH)/2, y, BUTTON_WIDTH, BUTTON_HEIGHT);
		g2.setColor(Color.BLACK);
		g2.drawString("SAVE", (getWidth() - BUTTON_WIDTH)/2 + 20, y + BUTTON_HEIGHT/2);
		g.drawImage(imgBuffer, 0, 0, getWidth(), getHeight(), this);
	}

	private boolean isInArea(int targetX,int targetY,int x,int y,int width,int height){
		return targetX <= x + width
				&& targetX >= x
				&& targetY >= y
				&& targetY <= y + height;
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
		int my = e.getY();
		int mx = e.getX();
		int y = getHeight() / 5;
		int x = getWidth() * 2 / 5;
		int maxLength = 0;
		for(Config c:configs){
			int tmp = c.getExplain().length();
			if(tmp > maxLength)
				maxLength = tmp;
		}
		for(Config c:configs){
			if(isInArea(mx, my, x - 5, y,
					maxLength * 22 + 19 + BUTTON_WIDTH, BUTTON_HEIGHT)){
				c.toggleFlag();
				return;
			}
			y += BUTTON_HEIGHT + 20;
		}
		if(isInArea(mx,my,(getWidth() - BUTTON_WIDTH)/2,y,
				BUTTON_WIDTH,BUTTON_HEIGHT))
			getFrame().setPage("start");
	}

	@Override
	public void mousePressed(MouseEvent e) {}
	@Override
	public void mouseReleased(MouseEvent e) {}
	@Override
	public void mouseEntered(MouseEvent e) {}
	@Override
	public void mouseExited(MouseEvent e) {}
	private void saveConfig(){}
	@Override
	public void setPreferredSize(Dimension d) {
		super.setPreferredSize(d);
	}

	@Override
	public void movePage(String order) {
		getFrame().setPage(order);
	}

	@Override
	public Client getOperator() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public String getPageName(){
		return "Config";
	}
	@Override
	public boolean isFinish() {
		// TODO Auto-generated method stub
		return isFinish;
	}
	@Override
	public void finish() {
		// TODO Auto-generated method stub
		isFinish = true;
	}
	@Override
	public String getNextPageName() {
		// TODO Auto-generated method stub
		return StartPage.class.getName();
	}
}
