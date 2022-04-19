/*
 * Filename: MagViewer.java
 * This is the main class which shows the main application window
 */

package org.wormloco.mag;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import java.net.MalformedURLException;
import java.net.URL;

import javax.media.CachingControlEvent;
import javax.media.ConfigureCompleteEvent;
import javax.media.ControllerClosedEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerErrorEvent;
import javax.media.ControllerListener;
import javax.media.Effect;
import javax.media.EndOfMediaEvent;
import javax.media.Format;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.NoPlayerException;
import javax.media.Player;
import javax.media.PrefetchCompleteEvent;
import javax.media.Processor;
import javax.media.ProcessorModel;
import javax.media.RealizeCompleteEvent;

import javax.media.control.TrackControl;

import javax.media.format.VideoFormat;

import javax.media.protocol.ContentDescriptor;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class MagViewer extends JPanel implements ActionListener, ControllerListener {

	// serial version UID
	private static final long serialVersionUID = 1L;

	/** the version of this software */
	public static final String VERSION = "MagViewer 11/2/2014";

	/** constant for analyze-video menu option */
	public static final String ANALYZE_ONE_VIDEO = "Analyze Video";

	/** constant for quit menu-option */
	public static final String QUIT = "Quit";

	/** constant for open menu-option */
	public static final String OPEN = "Open ...";

	/** constant for head-angle options-menu */
	public static final String HEAD_ANGLE = "Show/hide Head Angle";

	/** constant for body-angle options-menu */
	public static final String BODY_ANGLE = "Show/hide Body Angle";

	/** constant for stride-info options-menu */
	public static final String STRIDE_INFO = "Show/hide Stride Information";

	// remember the parent frame
	private final JFrame parentFrame;

	// file-chooser
	protected final JFileChooser videoFileChooser;

	// panel for playing video
	private final JPanel imagePanel = new JPanel( true );

	// panel for video controls
	private final JPanel controlsPanel = new JPanel();

	// panel for drawing-track
	private final JPanel trackPanel = new JPanel( true );

	// panel for body-size
	private final JPanel bodySizePanel = new JPanel();

	// panel for velocity
	private final JPanel velocityPanel = new JPanel();

	// checkbox-menu for head-angle
	private final JCheckBoxMenuItem headLineMenuItem = new JCheckBoxMenuItem( HEAD_ANGLE );

	// checkbox-menu for body-angle
	private final JCheckBoxMenuItem bodyLineMenuItem = new JCheckBoxMenuItem( BODY_ANGLE );

	// checkbox-menu for stride-info
	private final JCheckBoxMenuItem strideInfoMenuItem = new JCheckBoxMenuItem( STRIDE_INFO );

	// the video file
	private File videoFile;

	// the player object
	private Player player;

	// the processor object
	private Processor processor;

	// default background image
	private ImageIcon imageIcon;
    
	// for convenience on println statements
	private static final PrintStream out = System.out;

	// controls gain, position, start, stop
	private Component controlComponent = null;

	// control panel height
	private int controlPanelHeight = 0;

	// component in which video is playing
	private Component visualComponent = null;

	// the actual video data
	private Video video = new Video();

	// video-width
	private int videoWidth = 0;

	// video-height
	private int videoHeight = 0;

	// used for syncrhonized sections
	private boolean stateTransitionOK = true;

	// sync object
	private Object waitSync = new Object();


	/**
	 * Constructor
	 * @param  parent  the parent frame
	 */
	 public MagViewer( JFrame parent ) {
		this.parentFrame = parent;

		File tmp = new File( "images/file.120overlay.jpg" );
		if( tmp.exists() == true ) {
			imageIcon = new ImageIcon( tmp.getAbsolutePath() );
		}
		else {
			tmp = new File( "../images/file.120overlay.jpg" );
			if( tmp.exists() == true ) {
				imageIcon = new ImageIcon( tmp.getAbsolutePath() );
			}
			else {
				imageIcon = new ImageIcon();
			}; // if
		}; // if

		// menu
		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu( "File" );
		parentFrame.setJMenuBar( menuBar );
		menuBar.add( fileMenu );
		fileMenu.setMnemonic( KeyEvent.VK_F );

		JMenuItem openMenuItem = new JMenuItem( OPEN );
		JMenuItem quitMenuItem = new JMenuItem( QUIT );

		openMenuItem.setActionCommand( OPEN );
		quitMenuItem.setActionCommand( QUIT );

		openMenuItem.addActionListener( this );
		quitMenuItem.addActionListener( this );

		openMenuItem.setMnemonic( KeyEvent.VK_O );
		quitMenuItem.setMnemonic( KeyEvent.VK_Q );

		fileMenu.add( openMenuItem );
		fileMenu.add( quitMenuItem );

		JMenu optionsMenu = new JMenu( "Options" );
		optionsMenu.setMnemonic( KeyEvent.VK_O );
		menuBar.add( optionsMenu );
		// head line menu item
		headLineMenuItem.setMnemonic( KeyEvent.VK_H );
		headLineMenuItem.setActionCommand( HEAD_ANGLE );
		headLineMenuItem.addActionListener( this );
		headLineMenuItem.setSelected( false );
		// body line menu item
		bodyLineMenuItem.setMnemonic( KeyEvent.VK_B );
		bodyLineMenuItem.setActionCommand( BODY_ANGLE );
		bodyLineMenuItem.addActionListener( this );
		bodyLineMenuItem.setSelected( false );
		// stride info menu item
		strideInfoMenuItem.setMnemonic( KeyEvent.VK_S );
		strideInfoMenuItem.setActionCommand( STRIDE_INFO );
		strideInfoMenuItem.addActionListener( this );
		strideInfoMenuItem.setSelected( true );
		// menu items added to menu
		optionsMenu.add( headLineMenuItem );
		optionsMenu.add( bodyLineMenuItem );
		optionsMenu.add( strideInfoMenuItem );

		// layout
		setLayout( new GridBagLayout() );

		controlsPanel.setPreferredSize( new Dimension( 570, 24 ) );

		// buttons panel
		JPanel buttonsPanel = new JPanel( new GridBagLayout() );
		JButton playButton = new JButton( "Play" );
		playButton.setActionCommand( "Play" );
		playButton.addActionListener( this );
		playButton.setMnemonic( KeyEvent.VK_P );
		buttonsPanel.add( playButton, new GBC( 0, 0 ) );
		buttonsPanel.add( controlsPanel, new GBC( 1, 0 ) );

		// video-image panel
		imagePanel.add( new JLabel( imageIcon ) );
		imagePanel.setPreferredSize( new Dimension( 640, 480 ) );
		imagePanel.setName( "" );
		JPanel videoPanel = new JPanel( new GridBagLayout() );
		videoPanel.add( buttonsPanel, new GBC( 0, 0 ) );
		videoPanel.add( imagePanel, new GBC( 0, 1 ) );

		trackPanel.setBorder( BorderFactory.createEtchedBorder() );
		trackPanel.setPreferredSize( new Dimension( 310, 310 ) );
		bodySizePanel.setBorder( BorderFactory.createEtchedBorder() );
		bodySizePanel.setPreferredSize( new Dimension( 420, 180 ) );
		velocityPanel.setBorder( BorderFactory.createEtchedBorder() );
		velocityPanel.setPreferredSize( new Dimension( 320, 180 ) );

		add( videoPanel, new GBC( 0, 0 ).setSpan( 1, 1 ) );
		add( bodySizePanel, new GBC( 0, 1 ).setDefaultInsets() );
		add( velocityPanel, new GBC( 1, 1 ).setDefaultInsets() );
		add( trackPanel, new GBC( 1, 0 ).setDefaultInsets() );

		// set up the file-chooser
		if( "/".equals( File.separator ) == true ) {
			videoFileChooser = new JFileChooser( System.getProperty( "user.home" ) );
		} 
		else {
			videoFileChooser = new JFileChooser( "c:\\data" );
		}; // if
		videoFileChooser.setFileFilter( new FileFilterVideo() );
		videoFileChooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
		videoFile = null;
		player = null;
	}


	/**
	 * Create the GUI and show it
	 */
	private static void createAndShowGUI() {
		JFrame frame = new JFrame( VERSION );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

		// create the actual object (of the class MagViewer)
		MagViewer gui = new MagViewer( frame );
		gui.setOpaque( true );
		frame.setContentPane( gui );

		// display the window
		frame.pack();
		frame.setVisible( true );
	}
      
	
	/** 
	 * Trick to tell position-effect whether to show angle lines, and stride info
	 */
	public void trickToTellPositionEffectWhetherToShowAngles() {
		String line = "";
		if( headLineMenuItem.isSelected() == true ) {
			line = HEAD_ANGLE;
		}; // if
		line += "|";
		if( bodyLineMenuItem.isSelected() == true ) {
			line += BODY_ANGLE;
		}; // if
		line += "|";
		if( strideInfoMenuItem.isSelected() == true ) {
			line += STRIDE_INFO;
		}; // if
		imagePanel.setName( line );
	}
     
	/** 
	 * Actions of buttons take place here
	 * @param  actionEvent  the action-event object
	 */
	public void actionPerformed( ActionEvent actionEvent ) {
		if( OPEN.equals( actionEvent.getActionCommand() ) == true ) {
			int returnValue = videoFileChooser.showOpenDialog( this );
			if( returnValue != JFileChooser.APPROVE_OPTION ) {
				videoFile  = null;
				return;
			}; // if
			videoFile = videoFileChooser.getSelectedFile();
		}; // if

		if( HEAD_ANGLE.equals( actionEvent.getActionCommand() ) == true 
		||  BODY_ANGLE.equals( actionEvent.getActionCommand() ) == true 
		||  STRIDE_INFO.equals( actionEvent.getActionCommand() ) == true ) {
			trickToTellPositionEffectWhetherToShowAngles();
		}; // if

		if( QUIT.equals( actionEvent.getActionCommand() ) == true ) {
			parentFrame.setVisible( false );
			parentFrame.dispose();
		}; // if

		if( "Play".equals( actionEvent.getActionCommand() ) == true ) {
			parentFrame.setTitle( VERSION );
			if( videoFile == null ) {
				int returnValue = videoFileChooser.showOpenDialog( this );
				if( returnValue != JFileChooser.APPROVE_OPTION ) {
					videoFile  = null;
					return;
				}; // if
				videoFile = videoFileChooser.getSelectedFile();
			}; // if
			out.println( "video: " + videoFile );
			URL url = null;
			String mediaFile = null;
			try {
				url = new URL( "file:///" + videoFile.getAbsolutePath() );
				mediaFile = url.toExternalForm();
			} 
			catch( MalformedURLException mue ) {
				mue.printStackTrace();
				JOptionPane.showMessageDialog( null, "Unable to play video.", "Unable to play video.", JOptionPane.ERROR_MESSAGE );
				return;
			}; // try

			// Create a media locator from the file name
			MediaLocator mediaLocator = null;
			player = null;
			if( ( mediaLocator = new MediaLocator( mediaFile ) ) == null ) {
				JOptionPane.showMessageDialog( null, "Unable to play video (media-locator).", "Unable to play video (media-locator).", JOptionPane.ERROR_MESSAGE );
				return;
			}; // if
			
			Format[] formats = new Format[ 1 ];
			formats[ 0 ] = new VideoFormat( VideoFormat.CINEPAK );
			ContentDescriptor contentDescriptor = null;
			ProcessorModel processorModel = new ProcessorModel( mediaLocator, formats, contentDescriptor );
				
			try {
				processor = Manager.createProcessor( mediaLocator );
				parentFrame.setTitle( VERSION + "   " + videoFile.getAbsolutePath() );
			} 
			catch( Exception e ) {
				e.printStackTrace();
				JOptionPane.showMessageDialog( null, "Unable to play video (create-processor).", "Unable to play video (create-processor).", JOptionPane.ERROR_MESSAGE );
				return;
			}; // try
				
			processor.addControllerListener( this );
			processor.configure();
		}; // if
	}
	

	/**
	 * Start media file playback. This function is called the
	 * first time that the Applet runs and every
	 * time the user re-enters the page.
	 */
	public void start() {
		if( player != null ) {
			//out.println( "player starts" );
			player.start();
			//out.println( "player started" );
		}; // if
	}

	/**
	 * This controllerUpdate function must be defined in order to
	 * implement a ControllerListener interface. This 
	 * function will be called whenever there is a media event
	 */
	public synchronized void controllerUpdate( ControllerEvent event ) {

		if( event instanceof ConfigureCompleteEvent ) {
			//out.println( "ConfigureCompleteEvent" );
			video.resetEverything();
			video.setDirectory( videoFile.getParent() );
			String error = video.calculateAllParameters();
			if( error != null ) {
				out.println( "\t" + "Error in Video.java: " + error );
				JOptionPane.showMessageDialog( null, "Unable to play video.\nError:\n" + error , "Unable to play video.", JOptionPane.ERROR_MESSAGE );
				return;
			}; // if
			error = video.writeDetailsTextfile();
			if( error != null ) {
				out.println( "\t" + "Error inside Video.java: " + error );
			}; // if

			processor.setContentDescriptor( null );
			TrackControl[] trackControls = processor.getTrackControls();
			trickToTellPositionEffectWhetherToShowAngles();
			for( int i = 0; i < trackControls.length; i++ ) {
				if( trackControls[ i ].getFormat() instanceof VideoFormat ) {
					trackControls[ i ].setFormat( new VideoFormat( VideoFormat.CINEPAK ) );
					Effect[] effects = { 
						new TrackEffect( videoFile, trackPanel, video ), 
						new PositionEffect( videoFile, imagePanel, video ),
						new PlotBodySizeEffect( videoFile, bodySizePanel, video ), 
						new VelocityEffect( videoFile, velocityPanel, video ), 
						};
					try {
						trackControls[ i ].setCodecChain( effects );
					}
					catch( Exception e ) {
						e.printStackTrace();
					}; // try
				}; // if
			}; // for
			processor.realize();
		}
		else if( event instanceof RealizeCompleteEvent ) {
			//out.println( "RealizeCompleteEvent" );
			processor.prefetch();
		}
		else if( event instanceof PrefetchCompleteEvent ) {
			int height = 0;
			int width = 600;
			if( controlComponent == null ) {
				if( ( controlComponent = processor.getControlPanelComponent() ) != null ) {
					controlPanelHeight = controlComponent.getPreferredSize().height;
					controlsPanel.add( controlComponent );
					height += controlPanelHeight;
					//out.println( "added control-component, height: " + height );
				}; // if
			}; // if
			if( visualComponent == null ) {
				if( ( visualComponent = processor.getVisualComponent() ) != null ) {
					//imagePanel.removeAll();
					//imagePanel.add( visualComponent );
					Dimension videoSize = visualComponent.getPreferredSize();
					videoWidth = videoSize.width;
					width = videoWidth;
				}; // if
			}; // if
			if( controlComponent != null ) {
				controlComponent.setPreferredSize( new Dimension( videoWidth - 80, controlPanelHeight ) );
				controlComponent.invalidate();
				parentFrame.pack();
			}; // if
			processor.start();
		} 
		else if( event instanceof EndOfMediaEvent ) {
			//out.println( "EndOfMediaEvent" );
			// We've reached the end of the media
			//player.setMediaTime(new Time(0));
		} 
		else if( event instanceof ControllerErrorEvent ) {
			//out.println( "ControllerErrorEvent" );
			player = null;
			JOptionPane.showMessageDialog( null, "Unable to play media (controller-error).\n" + ( ( ControllerErrorEvent ) event ).getMessage() , "Unable to play media (controller-error).", JOptionPane.ERROR_MESSAGE );
		} 
		else if( event instanceof ControllerClosedEvent ) {
			//out.println( "ControllerClosedEvent" );
			imagePanel.removeAll();
			imagePanel.add( new JLabel( imageIcon ) );
			imagePanel.invalidate();
			parentFrame.pack();
		}
		else {
			//out.println( "---" + event + "---" );
		}; // if
	}


	/**
	 * Stop media file playback and release resource before
	 * leaving the page.
	 */
	public void stop() {
		if( player != null ) {
			player.stop();
			player.deallocate();
			imagePanel.removeAll();
			imagePanel.add( new JLabel( imageIcon ) );
			imagePanel.invalidate();
			parentFrame.pack();
		}; // if
	}

	boolean waitForState(int state) {
		synchronized( waitSync ) {
			try {
				while( player.getState() != state && stateTransitionOK ) {
					//System.out.println( "Player state: " + player.getState() );
					waitSync.wait();                  
				}; // while
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			} catch (Exception e) {
				//System.out.println("waiting and then exception happens!");
				e.printStackTrace();
			}; // try
		}; // synchronized
		return stateTransitionOK;
	}

	/** Runs the application via a runnable invocation */
	public static void main( String[] args ) {
		SwingUtilities.invokeLater( new Runnable() {
			public void run() {
				try {
					UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
				}
				catch( Exception ignore ) {
					ignore.printStackTrace();
					return;
				}; // try
				createAndShowGUI();
			}
		} );
	}

} // class MagViewer

