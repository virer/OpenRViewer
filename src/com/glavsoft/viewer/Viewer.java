// Copyright (C) 2010 - 2014 GlavSoft LLC.
// All rights reserved.
//
// -----------------------------------------------------------------------
// This file is part of the TightVNC software.  Please visit our Web site:
//
//                       http://www.tightvnc.com/
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, write to the Free Software Foundation, Inc.,
// 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
// -----------------------------------------------------------------------
//
package com.glavsoft.viewer;

import com.glavsoft.exceptions.CommonException;
import com.glavsoft.rfb.protocol.ProtocolSettings;
import com.glavsoft.utils.LazyLoaded;
import com.glavsoft.utils.Strings;
import com.glavsoft.viewer.cli.Parser;
import com.glavsoft.viewer.mvp.View;
import com.glavsoft.viewer.settings.ConnectionParams;
import com.glavsoft.viewer.settings.UiSettings;
import com.glavsoft.viewer.swing.*;
import com.glavsoft.viewer.swing.gui.ConnectionDialogView;
import com.glavsoft.viewer.swing.gui.ConnectionInfoView;
import com.glavsoft.viewer.swing.gui.ConnectionView;
import com.glavsoft.viewer.swing.mac.MacApplicationWrapper;
import com.glavsoft.viewer.swing.mac.MacUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("serial")
public class Viewer extends JApplet implements Runnable, MouseListener, ViewerEventsListener {

    private static final String ATTR_APPLET_GOODBYE_URL = "AppletGoodbyeURL";
    private static final String DEFAULT_APPLET_GOODBYE_URL = "about:blank";
    private final ApplicationSettings applicationSettings;
    private static final Logger logger = Logger.getLogger(Viewer.class.getName());
    private int paramsMask;
    private boolean allowAppletInteractiveConnections;

    private final ConnectionParams connectionParams;
    private String passwordFromParams;
    private boolean isSeparateFrame = true;
    private boolean isApplet = true; 
    private final ProtocolSettings settings;
    private final UiSettings uiSettings;
    private volatile boolean isAppletStopped = false;
    private ConnectionPresenter connectionPresenter;
    private MouseEnteredListener mouseEnteredListener;

    public static void StartViewer(String[] args) {  // XXX OpenRViewer modified => old main()
        if (MacUtils.isMac()) {
            // do mac os specific things
            try {
                MacApplicationWrapper application = MacApplicationWrapper.getApplication();
                application.setEnabledAboutMenu(false);
                MacUtils.setName("TightVNC Viewer");
                application.setDockIconImage(MacUtils.getIconImage());
            } catch (CommonException e) {
                logger.warning(e.getMessage());
            }
        }
        Parser parser = new Parser();
		ParametersHandler.completeParserOptions(parser);

		parser.parse(args);
		if (parser.isSet(ParametersHandler.ARG_HELP)) {
			printUsage(parser.optionsUsage());
			System.exit(0);
		}
		Viewer viewer = new Viewer(parser);
		SwingUtilities.invokeLater(viewer);
	}

    private static void printUsage(String additional) {
		System.out.println("Usage: java -jar (progfilename) [hostname [port_number]] [Options]\n" +
                "    or\n" +
                " java -jar (progfilename) [Options]\n" +
                "    or\n java -jar (progfilename) -help\n    to view this help\n\n" +
                "Where Options are:\n" + additional +
                "\nOptions format: -optionName=optionValue. Ex. -host=localhost -port=5900 -viewonly=yes\n" +
                "Both option name and option value are case insensitive.");
	}

	public Viewer() {
        logger.info("TightVNC Viewer version " + ver());
		connectionParams = new ConnectionParams();
		settings = ProtocolSettings.getDefaultSettings();
		uiSettings = new UiSettings();
        applicationSettings = new ApplicationSettings();
	}

	private Viewer(Parser parser) {
		this();
        paramsMask = ParametersHandler.completeSettingsFromCLI(
                parser, connectionParams, settings, uiSettings, applicationSettings);
        setLoggingLevel(applicationSettings.logLevel);
		passwordFromParams = applicationSettings.password;
		isApplet = false;
	}

    private void setLoggingLevel(Level levelToSet) {
        final Logger appLogger = Logger.getLogger("com.glavsoft");
        try {
            appLogger.setUseParentHandlers(false);
            appLogger.setLevel(levelToSet);
            for (Handler h : appLogger.getHandlers()) {
                if (h instanceof ConsoleHandler) {
                    appLogger.removeHandler(h);
                } else {
                    h.setLevel(levelToSet);
                }
            }
            ConsoleHandler ch = new ConsoleHandler();
            ch.setLevel(levelToSet);
            appLogger.addHandler(ch);
        } catch (SecurityException e) {
            logger.warning("cannot set logging level to: " + levelToSet);
        }
    }

	/**
	 * Closes App(lication) or stops App(let).
	 */
    private void closeApp() {
        if (connectionPresenter != null) {
            connectionPresenter.cancelConnection();
            logger.info("Connection cancelled.");
        }
        if (isApplet) {
            logger.severe("Applet is stopped.");
            isAppletStopped = true;
            repaint();
            stop();
            URL goodbyeUrl = null;
            final String goodBye = getParameter(ATTR_APPLET_GOODBYE_URL);
            if (Strings.isTrimmedEmpty(goodBye)) return;
            try {
                goodbyeUrl = new URL(goodBye);
            } catch (MalformedURLException e) {
                logger.severe("Malformed URL: '" + goodBye + "'. Using '" + DEFAULT_APPLET_GOODBYE_URL + "'");
                try {
                    goodbyeUrl = new URL(DEFAULT_APPLET_GOODBYE_URL);
                } catch (MalformedURLException ignore) {
                }
            }
            getAppletContext().showDocument(goodbyeUrl);
        } else {
			System.exit(0);
		}
	}

	@Override
	public void paint(Graphics g) {
		if ( ! isAppletStopped) {
			super.paint(g);
		} else {
			getContentPane().removeAll();
			g.clearRect(0, 0, getWidth(), getHeight());
			g.drawString("Disconnected", 10, 20);
		}
	}

	@Override
	public void destroy() {
		closeApp();
		super.destroy();
	}

	@Override
	public void init() {
        AppletSettings appletSettings = new AppletSettings();
		paramsMask = ParametersHandler.completeSettingsFromApplet(
                this, connectionParams, settings, uiSettings, applicationSettings, appletSettings);
        setLoggingLevel(applicationSettings.logLevel);
		isSeparateFrame = appletSettings.isSeparateFrame;
		passwordFromParams = applicationSettings.password;
		isApplet = true;
        allowAppletInteractiveConnections = appletSettings.allowInteractiveConnections;
		repaint();

        try {
            SwingUtilities.invokeAndWait(this);
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
    }

	@Override
	public void start() {
		super.start();
	}

    @Override
	public void run() {
        if (isApplet && allowAppletInteractiveConnections) uiSettings.showConnectionDialog = true;
        if (isApplet && ! allowAppletInteractiveConnections) uiSettings.showConnectionDialog = false; 
        connectionPresenter = new ConnectionPresenter();
        connectionPresenter.addModel("ConnectionParamsModel", connectionParams);

        final ConnectionView connectionView = uiSettings.showConnectionDialog ?
                new ConnectionDialogView(Viewer.this, connectionPresenter) :
                new ConnectionInfoView(Viewer.this, connectionPresenter);
        connectionPresenter.addView(ConnectionPresenter.CONNECTION_VIEW, connectionView);

        if (isApplet) {
            connectionPresenter.addView("AppletStatusStringView", new AppletStatusStringView());
        }

        SwingViewerWindowFactory viewerWindowFactory = new SwingViewerWindowFactory(isSeparateFrame, this);
        viewerWindowFactory.setAppName("TightVNC Viewer v."+Viewer.ver());
        if (isApplet) {
            viewerWindowFactory.setIsApplet(true);
            viewerWindowFactory.setExternalContainer(this);
        }

        connectionPresenter.setConnectionWorkerFactory(
                new SwingConnectionWorkerFactory(
                        connectionView.getFrame(),
                        passwordFromParams, connectionPresenter, viewerWindowFactory));

        connectionPresenter.startConnection(settings, uiSettings, paramsMask);
//        connectionPresenter.getViewerControlApi();
    }

//    @Override
//    public void windowClosing(WindowEvent e) {
//        if (e != null && e.getComponent() != null) {
//            final Window w = e.getWindow();
//            if (w != null) {
//                w.setVisible(false);
//                w.dispose();
//            }
//        }
//        closeApp();
//    }
//    @Override
//	public void windowOpened(WindowEvent e) { /* nop */ }
//	@Override
//	public void windowClosed(WindowEvent e) { /* nop */ }
//	@Override
//	public void windowIconified(WindowEvent e) { /* nop */ }
//	@Override
//	public void windowDeiconified(WindowEvent e) { /* nop */ }
//	@Override
//	public void windowActivated(WindowEvent e) { /* nop */ }
//	@Override
//	public void windowDeactivated(WindowEvent e) { /* nop */ }

    private static LazyLoaded<String> ver = new LazyLoaded<String>(new LazyLoaded.Loader<String>() {
        @Override
        public String load() {
            String version = Viewer.class.getPackage().getImplementationVersion();

            if (version != null) {
                return version;
            }
            try {
                String result;
                Attributes attrs = new Manifest(new FileInputStream(JarFile.MANIFEST_NAME)).getMainAttributes();
                final String ver = attrs.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
                try {
                    result = ver != null ? new String(ver.getBytes("ISO-8859-1"), "ISO-8859-1") : null;
                } catch (UnsupportedEncodingException e) {
                    result = null;
                }
                version = result;
            } catch (FileNotFoundException e) {
                System.out.println("Manifest file not found");
            } catch (IOException e) {
                System.out.println("Cannot read Manifest file");
            }
            return version;
        }
    });

    public static String ver() {
        return ver.get();
    }

    /* this is to prevent input loss in java-applet embedded mode */
    @Override
    public void mouseClicked(MouseEvent mouseEvent) { /* nop */ }
    @Override
    public void mousePressed(MouseEvent mouseEvent) { /* nop */ }
    @Override
    public void mouseReleased(MouseEvent mouseEvent) { /* nop */ }
    @Override
    public void mouseExited(MouseEvent mouseEvent) { /* nop */ }
    @Override
    public void mouseEntered(MouseEvent mouseEvent) {
        if (mouseEnteredListener != null) {
            mouseEnteredListener.mouseEnteredEvent(mouseEvent);
        }
    }

    @Override
    public void onViewerComponentClosing() {
        closeApp();
    }

    @Override
    public void onViewerComponentContainerBuilt(SwingViewerWindow viewerWindow) {
        if (isSeparateFrame) {
            viewerWindow.setVisible();
        }
        viewerWindow.validate();
        mouseEnteredListener = viewerWindow;
        viewerWindow.addMouseListener(this);
    }

    private void moveFrameToFrontABitLater(final JFrame frame) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // nop
                }
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        frame.toFront();
                    }
                });
            }
        }).start();
    }

    private class AppletStatusStringView implements View {
        @Override
        public void showView() { /*nop*/ }

        @Override
        public void closeView() { /*nop*/ }

        @SuppressWarnings("UnusedDeclaration")
        public void setMessage(String message) {
            Viewer.this.getAppletContext().showStatus(message);
        }
    }

    public static class AppletSettings {
        public boolean allowInteractiveConnections;
        public boolean isSeparateFrame;
    }

    public static class ApplicationSettings {
        Level logLevel;
        public String password;
        public void calculateLogLevel(boolean verbose, boolean verboseMore) {
            logLevel = verboseMore ? Level.FINER :
                    verbose ? Level.FINE :
                            Level.INFO;
        }
    }
}
