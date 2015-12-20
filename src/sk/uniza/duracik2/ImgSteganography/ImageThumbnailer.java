/*
 *  Copyright (c) 2015 Michal Ďuračík
 */
package sk.uniza.duracik2.ImgSteganography;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JComponent;

/**
 *
 * @author Unlink
 */
public class ImageThumbnailer extends JComponent {

	private File aImgFile;
	private BufferedImage aBuffer;
	
	public ImageThumbnailer() {
		super();
	}

	public void setImgFile(File paImgFile) {
		aImgFile = paImgFile;
		aBuffer = null;
		repaint();
	}

	@Override
	protected void paintComponent(Graphics paG) {
		super.paintComponent(paG);
		Graphics2D g = (Graphics2D) paG;
		
		if (aImgFile == null) {
			int x = this.getWidth()/2 - 50;
			int y = this.getHeight()/2;
			g.drawString("Otvorte obrázok", x, y);
		}
		else {
			prepareImage();
			if (aBuffer == null) {
				int x = this.getWidth()/2 - 70;
				int y = this.getHeight()/2;
				g.drawString("Nepodarilo sa načítať náhľad", x, y);
			}
			else {
				double scale = 1;
				int w = aBuffer.getWidth();
				int h = aBuffer.getHeight();
				if (w > this.getWidth()) {
					scale = this.getWidth() / (double)w;
					w = this.getWidth();
					h *= scale;
				}
				
				if (h > this.getHeight()) {
					scale = this.getHeight()/ (double)h;
					h = this.getHeight();
					w *= scale;
				}
				
				int x = (this.getWidth() - w) / 2;
				int y = (this.getHeight()- h) / 2;
				g.drawImage(aBuffer, x, y, w, h, null);
				
				//Info o rozlíšení
				int odhad = (((((aBuffer.getWidth() * aBuffer.getHeight()) * 3) / 8 - 30)*256)/256);
				String resolution = aBuffer.getWidth()+"x"+aBuffer.getHeight()+" ~ "+odhad+" znakov";
				
				FontMetrics metrics = g.getFontMetrics(g.getFont());
				int adh = metrics.getHeight();
				int adv = metrics.stringWidth(resolution);
				
				g.setColor(Color.WHITE);
				g.fillRoundRect(x+w-adv-7, y+h-7-adh, adv+6, adh+6, 3, 3);
				g.setColor(Color.BLACK);
				g.drawString(resolution, x+w-adv-4, y+h-5);
			}
		}
		
	}

	private void prepareImage() {
		if (aBuffer == null) {
			try {
				aBuffer = ImageIO.read(aImgFile);
			}
			catch (IOException ex) {
				//Pass
			}
		}
	}
	
}
