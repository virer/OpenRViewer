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

import com.glavsoft.rfb.encoding.EncodingType;
import com.glavsoft.rfb.protocol.LocalPointer;
import com.glavsoft.rfb.protocol.ProtocolSettings;
import com.glavsoft.rfb.protocol.tunnel.TunnelType;
import com.glavsoft.utils.Strings;
import com.glavsoft.viewer.cli.Parser;
import com.glavsoft.viewer.settings.ConnectionParams;
import com.glavsoft.viewer.settings.UiSettings;
import com.glavsoft.viewer.settings.WrongParameterException;

import javax.swing.*;
import java.util.logging.Logger;

public class ParametersHandler {
	public static final String ARG_LOCAL_POINTER = "LocalPointer";
	public static final String ARG_SCALING_FACTOR = "ScalingFactor";
    public static final String ARG_FULL_SCREEN = "FullScreen";
	public static final String ARG_COLOR_DEPTH = "ColorDepth";
	public static final String ARG_JPEG_IMAGE_QUALITY = "JpegImageQuality";
	public static final String ARG_COMPRESSION_LEVEL = "CompressionLevel";
	public static final String ARG_ENCODING = "Encoding";
	public static final String ARG_SHARE_DESKTOP = "ShareDesktop";
	public static final String ARG_ALLOW_COPY_RECT = "AllowCopyRect";
	public static final String ARG_VIEW_ONLY = "ViewOnly";
	public static final String ARG_SHOW_CONTROLS = "ShowControls";
	public static final String ARG_OPEN_NEW_WINDOW = "OpenNewWindow";
	public static final String ARG_PASSWORD = "password";
	public static final String ARG_PORT = "port";
	public static final String ARG_HOST = "host";
	public static final String ARG_HELP = "help";
    public static final String ARG_VERBOSE = "v";
    public static final String ARG_VERBOSE_MORE = "vv";
	public static final String ARG_CONVERT_TO_ASCII = "ConvertToASCII";
	public static final String ARG_ALLOW_CLIPBOARD_TRANSFER = "AllowClipboardTransfer";
	public static final String ARG_REMOTE_CHARSET = "RemoteCharset";
	public static final String ARG_SSH_HOST = "sshHost";
	public static final String ARG_SSH_USER = "sshUser";
	public static final String ARG_SSH_PORT = "sshPort";
    public static final String ARG_ALLOW_APPLET_INTERACTIVE_CONNECTIONS = "AllowAppletInteractiveConnections";
    public static final String ARG_TUNNELING = "Tunneling";
    public static final String ARG_SHOW_CONNECTION_DIALOG = "showConnectionDialog";
    public static final String ARG_TARGET_SERVER_ID = "targetServerId";

    public static void completeParserOptions(Parser parser) {
		parser.addOption(ARG_HELP, null, "Print this help.");
		parser.addOption(ARG_HOST, "", "Server host name.");
		parser.addOption(ARG_PORT, "0", "Port number.");
		parser.addOption(ARG_PASSWORD, null, "Password to the server.");
		parser.addOption(ARG_SHOW_CONTROLS, null, "Set to \"No\" if you want to get rid of that " +
				"button panel at the top. Default: \"Yes\".");
        parser.addOption(ARG_SHOW_CONNECTION_DIALOG, null, "Set to \"No\" if you want not to show initial connection dialog." +
                " Default: \"Yes\".");
        parser.addOption(ARG_TARGET_SERVER_ID, null, "targetServerID must not be empty. Default null");
		parser.addOption(ARG_VIEW_ONLY, null, "When set to \"Yes\", then all keyboard and mouse " +
				"events in the desktop window will be silently ignored and will not be passed " +
				"to the remote side. Default: \"No\".");
		parser.addOption(ARG_ALLOW_CLIPBOARD_TRANSFER, null, "When set to \"Yes\", transfer of clipboard contents is allowed. " +
				"Default: \"Yes\".");
		parser.addOption(ARG_REMOTE_CHARSET, null, "Charset encoding is used on remote system. Use this option to specify character encoding will be used for encoding clipboard text content to. Default value: local system default character encoding. Set the value to 'standard' for using 'Latin-1' charset which is only specified by rfb standard for clipboard transfers.");
		parser.addOption(ARG_SHARE_DESKTOP, null, "Share the connection with other clients " +
				"on the same VNC server. The exact behaviour in each case depends on the server " +
				"configuration. Default: \"Yes\".");
		parser.addOption(ARG_ALLOW_COPY_RECT, null, "The \"CopyRect\" encoding saves bandwidth " +
				"and drawing time when parts of the remote screen are moving around. " +
				"Most likely, you don't want to change this setting. Default: \"Yes\".");
		parser.addOption(ARG_ENCODING, null, "The preferred encoding. Possible values: \"Tight\", " +
				"\"Hextile\", \"ZRLE\", and \"Raw\". Default: \"Tight\".");
		parser.addOption(ARG_COMPRESSION_LEVEL, null, "Use specified compression level for " +
				"\"Tight\" and \"Zlib\" encodings. Values: 1-9. Level 1 uses minimum of CPU " +
				"time on the server but achieves weak compression ratios. Level 9 offers best " +
				"compression but may be slow.");
        //noinspection ConstantConditions
        parser.addOption(ARG_JPEG_IMAGE_QUALITY, null, "Use the specified image quality level " +
				"in \"Tight\" encoding. Values: 1-9, Lossless. Default value: " +
				(ProtocolSettings.DEFAULT_JPEG_QUALITY > 0 ?
						String.valueOf(ProtocolSettings.DEFAULT_JPEG_QUALITY) :
						"\"Lossless\"")
				+ ". To prevent server of using " +
				"lossy JPEG compression in \"Tight\" encoding, use \"Lossless\" value here.");
		parser.addOption(ARG_LOCAL_POINTER, null, "Possible values: on/yes/true (draw pointer locally), off/no/false (let server draw pointer), hide). " +
		"Default: \"On\".");
		parser.addOption(ARG_CONVERT_TO_ASCII, null, "Whether to convert keyboard input to ASCII ignoring locale. Possible values: yes/true, no/false). " +
		"Default: \"No\".");
		parser.addOption(ARG_COLOR_DEPTH, null, "Bits per pixel color format. Possible values: 3 (for 8 colors), 6 (64 colors), 8 (256 colors), 16 (65 536 colors), 24 (16 777 216 colors), 32 (same as 24).");
		parser.addOption(ARG_SCALING_FACTOR, null, "Scale local representation of the remote desktop on startup. " +
				"The value is interpreted as scaling factor in percents. The default value of 100% " +
				"corresponds to the original framebuffer size.");
        parser.addOption(ARG_FULL_SCREEN, null, "Full screen mode. Possible values: yes/true and no/false. Default: no.");
		parser.addOption(ARG_SSH_HOST, "", "SSH host name.");
		parser.addOption(ARG_SSH_PORT, "0",
				"SSH port number. When empty, standard SSH port number (" + ConnectionParams.DEFAULT_SSH_PORT + ") is used.");
		parser.addOption(ARG_SSH_USER, "", "SSH user name.");
        parser.addOption(ARG_TUNNELING, "auto", "Tunneling. Possible values: auto - allow viewer to choose tunneling mode, none/no - no tunneling use, SSL - choose SSL tunneling when available. Default: auto");
        parser.addOption(ARG_ALLOW_APPLET_INTERACTIVE_CONNECTIONS, null, "");
//                "Allow applet interactively connect to other hosts then in HostName param or hostbase. Possible values: yes/true, no/false. Default: false.");
        parser.addOption(ARG_VERBOSE, null, "Verbose console output.");
        parser.addOption(ARG_VERBOSE_MORE, null, "More verbose console output.");

    }

	public static int completeSettingsFromCLI(final Parser parser, ConnectionParams connectionParams, ProtocolSettings rfbSettings, UiSettings uiSettings, Viewer.ApplicationSettings applicationSettings) {
		int mask = completeSettings(
				new ParamsRetriever() {
					@Override
					public String getParamByName(String name) {
                        if (ARG_VERBOSE.equalsIgnoreCase(name) || ARG_VERBOSE_MORE.equalsIgnoreCase(name)) {
                            return parser.isSet(name) ? name : null;
                        }
						return parser.getValueFor(name);
					}
				},
				connectionParams, rfbSettings, uiSettings, applicationSettings);
		// when hostName == a.b.c.d:3 where :3 is display num (X Window) we need add display num to port number
		if ( ! Strings.isTrimmedEmpty(connectionParams.hostName)) {
			splitConnectionParams(connectionParams, connectionParams.hostName);
		}
		if (parser.isSetPlainOptions()) {
			splitConnectionParams(connectionParams, parser.getPlainOptionAt(0));
			if (parser.getPlainOptionsNumber() > 1) {
                try {
                    connectionParams.setPortNumber(parser.getPlainOptionAt(1));
                } catch (WrongParameterException e) {
                    //nop
                }
            }
		}
        return mask;
	}


	/**
	 * Split host string into hostName + port number and set ConnectionParans.
	 * a.b.c.d:5000 -> hostName == a.b.c.d, portNumber == 5000
	 * a.b.c.d::5000 -> hostName == a.b.c.d, portNumber == 5000
	 */
	private static void splitConnectionParams(final ConnectionParams connectionParams, String host) {
		int indexOfColon = host.indexOf(':');
		if (indexOfColon > 0) {
			String[] splitted = host.split(":");
			connectionParams.hostName = splitted[0];
			if (splitted.length > 1) {
                try {
                    connectionParams.setPortNumber(splitted[splitted.length - 1]);
                } catch (WrongParameterException e) {
                    //nop
                }
            }
		} else {
			connectionParams.hostName = host;
		}
	}

	private interface ParamsRetriever {
		String getParamByName(String name);
	}
	private static int completeSettings(ParamsRetriever pr,
                                        ConnectionParams connectionParams,
                                        ProtocolSettings rfbSettings,
                                        UiSettings uiSettings,
                                        Viewer.ApplicationSettings applicationSettings) {
        completeConnectionSettings(pr, connectionParams);
        completeApplicationSettings(pr, applicationSettings);
        int uiMask = completeUiSettings(pr, uiSettings);
        int rfbMask = completeRfbSettings(pr, rfbSettings);
        return (uiMask << 16) | rfbMask;
	}

    private static int completeRfbSettings(ParamsRetriever pr, ProtocolSettings rfbSettings) {
        String viewOnlyParam = pr.getParamByName(ARG_VIEW_ONLY);
        String allowClipboardTransfer = pr.getParamByName(ARG_ALLOW_CLIPBOARD_TRANSFER);
        String remoteCharsetName = pr.getParamByName(ARG_REMOTE_CHARSET);
        String allowCopyRectParam = pr.getParamByName(ARG_ALLOW_COPY_RECT);
        String shareDesktopParam = pr.getParamByName(ARG_SHARE_DESKTOP);
        String encodingParam = pr.getParamByName(ARG_ENCODING);
        String compressionLevelParam = pr.getParamByName(ARG_COMPRESSION_LEVEL);
        String jpegQualityParam = pr.getParamByName(ARG_JPEG_IMAGE_QUALITY);
        String colorDepthParam = pr.getParamByName(ARG_COLOR_DEPTH);
        String localPointerParam = pr.getParamByName(ARG_LOCAL_POINTER);
        String convertToAsciiParam = pr.getParamByName(ARG_CONVERT_TO_ASCII);
        String tunneling = pr.getParamByName(ARG_TUNNELING);

        int rfbMask = 0;
        rfbSettings.setViewOnly(parseBooleanOrDefault(viewOnlyParam, false));
        if (isGiven(viewOnlyParam)) rfbMask |= ProtocolSettings.CHANGED_VIEW_ONLY;
        rfbSettings.setAllowClipboardTransfer(parseBooleanOrDefault(allowClipboardTransfer, true));
        if (isGiven(allowClipboardTransfer)) rfbMask |= ProtocolSettings.CHANGED_ALLOW_CLIPBOARD_TRANSFER;
        rfbSettings.setRemoteCharsetName(remoteCharsetName);
        rfbSettings.setAllowCopyRect(parseBooleanOrDefault(allowCopyRectParam, true));
        if (isGiven(allowCopyRectParam)) rfbMask |= ProtocolSettings.CHANGED_ALLOW_COPY_RECT;
        rfbSettings.setSharedFlag(parseBooleanOrDefault(shareDesktopParam, true));
        if (isGiven(shareDesktopParam)) rfbMask |= ProtocolSettings.CHANGED_SHARED;
        rfbSettings.setConvertToAscii(parseBooleanOrDefault(convertToAsciiParam, false));
        if (isGiven(convertToAsciiParam)) rfbMask |= ProtocolSettings.CHANGED_CONVERT_TO_ASCII;
        if (EncodingType.TIGHT.getName().equalsIgnoreCase(encodingParam)) {
			rfbSettings.setPreferredEncoding(EncodingType.TIGHT);
            rfbMask |= ProtocolSettings.CHANGED_ENCODINGS;
		}
        if (EncodingType.HEXTILE.getName().equalsIgnoreCase(encodingParam)) {
            rfbSettings.setPreferredEncoding(EncodingType.HEXTILE);
            rfbMask |= ProtocolSettings.CHANGED_ENCODINGS;
        }
        if (EncodingType.ZRLE.getName().equalsIgnoreCase(encodingParam)) {
            rfbSettings.setPreferredEncoding(EncodingType.ZRLE);
            rfbMask |= ProtocolSettings.CHANGED_ENCODINGS;
        }
        if (EncodingType.RAW_ENCODING.getName().equalsIgnoreCase(encodingParam)) {
            rfbSettings.setPreferredEncoding(EncodingType.RAW_ENCODING);
            rfbMask |= ProtocolSettings.CHANGED_ENCODINGS;
        }
        try {
			int compLevel = Integer.parseInt(compressionLevelParam);
            if (rfbSettings.setCompressionLevel(compLevel) == compLevel) {
                rfbMask |= ProtocolSettings.CHANGED_COMPRESSION_LEVEL;
			}
		} catch (NumberFormatException e) { /* nop */ }
        try {
            int jpegQuality = Integer.parseInt(jpegQualityParam);
            if (jpegQuality > 0 && jpegQuality <= 9) {
                rfbSettings.setJpegQuality(jpegQuality);
                rfbMask |= ProtocolSettings.CHANGED_JPEG_QUALITY;
            }
        } catch (NumberFormatException e) {
            if ("lossless".equalsIgnoreCase(jpegQualityParam)) {
                rfbSettings.setJpegQuality( - Math.abs(rfbSettings.getJpegQuality()));
            }
        }
        try {
            int colorDepth = Integer.parseInt(colorDepthParam);
            rfbSettings.setColorDepth(colorDepth);
            rfbMask |= ProtocolSettings.CHANGED_COLOR_DEPTH;
        } catch (NumberFormatException e) { /* nop */ }

        if ("on".equalsIgnoreCase(localPointerParam) ||
			"true".equalsIgnoreCase(localPointerParam) ||
			"yes".equalsIgnoreCase(localPointerParam)) {
				rfbSettings.setMouseCursorTrack(LocalPointer.ON);
                rfbMask |= ProtocolSettings.CHANGED_MOUSE_CURSOR_TRACK;
		}
        if ("off".equalsIgnoreCase(localPointerParam) ||
            "no".equalsIgnoreCase(localPointerParam) ||
            "false".equalsIgnoreCase(localPointerParam)) {
                rfbSettings.setMouseCursorTrack(LocalPointer.OFF);
            rfbMask |= ProtocolSettings.CHANGED_MOUSE_CURSOR_TRACK;
        }
        if ("hide".equalsIgnoreCase(localPointerParam) ||
            "hidden".equalsIgnoreCase(localPointerParam)) {
                rfbSettings.setMouseCursorTrack(LocalPointer.HIDE);
            rfbMask |= ProtocolSettings.CHANGED_MOUSE_CURSOR_TRACK;
        }
        if ("none".equalsIgnoreCase(tunneling) || "no".equalsIgnoreCase(tunneling) ||
                "false".equalsIgnoreCase(tunneling)) {
            rfbSettings.setTunnelType(TunnelType.NOTUNNEL);
        } else { // if ("ssl".equalsIgnoreCase(tunneling)) {
            rfbSettings.setTunnelType(TunnelType.SSL);
        }
        return rfbMask;
    }

    private static int completeUiSettings(ParamsRetriever pr, UiSettings uiSettings) {
        int uiMask = 0;
        String scaleFactorParam = pr.getParamByName(ARG_SCALING_FACTOR);
        String fullScreenParam = pr.getParamByName(ARG_FULL_SCREEN);
        uiSettings.showControls = parseBooleanOrDefault(pr.getParamByName(ARG_SHOW_CONTROLS), true);
        uiSettings.showConnectionDialog = parseBooleanOrDefault(pr.getParamByName(ARG_SHOW_CONNECTION_DIALOG), true);
        if (scaleFactorParam != null) {
			try {
				int scaleFactor = Integer.parseInt(scaleFactorParam.replaceAll("\\D", ""));
				if (scaleFactor >= 10 && scaleFactor <= 200) {
					uiSettings.setScalePercent(scaleFactor);
                    uiMask |= UiSettings.CHANGED_SCALE_FACTOR;
				}
			} catch (NumberFormatException e) { /* nop */ }
		}
        uiSettings.setFullScreen(parseBooleanOrDefault(fullScreenParam, false));
        if (isGiven(fullScreenParam)) uiMask |= UiSettings.CHANGED_FULL_SCREEN;
        return uiMask;
    }

    private static void completeApplicationSettings(ParamsRetriever pr, Viewer.ApplicationSettings applicationSettings) {
        applicationSettings.password = pr.getParamByName(ARG_PASSWORD);
        applicationSettings.calculateLogLevel(
                !Strings.isTrimmedEmpty(pr.getParamByName(ARG_VERBOSE)),
                !Strings.isTrimmedEmpty(pr.getParamByName(ARG_VERBOSE_MORE)));
    }

    private static void completeConnectionSettings(ParamsRetriever pr, ConnectionParams connectionParams) {
        connectionParams.hostName = pr.getParamByName(ARG_HOST);
        try {
            connectionParams.setPortNumber(pr.getParamByName(ARG_PORT));
        } catch (WrongParameterException e) {
            Logger.getLogger(ParametersHandler.class.getName()).warning(e.getMessage());
        }
        String sshHostNameParam = pr.getParamByName(ARG_SSH_HOST);
        connectionParams.sshHostName = sshHostNameParam;
        connectionParams.setUseSsh( ! Strings.isTrimmedEmpty(sshHostNameParam));
        try {
            connectionParams.setSshPortNumber(pr.getParamByName(ARG_SSH_PORT));
        } catch (WrongParameterException e) {
            Logger.getLogger(ParametersHandler.class.getName()).warning(e.getMessage());
        }
        connectionParams.sshUserName = pr.getParamByName(ARG_SSH_USER);
        connectionParams.targetServerID = pr.getParamByName(ARG_TARGET_SERVER_ID);
    }

    private static boolean isGiven(String param) {
        return ! Strings.isTrimmedEmpty(param);
    }

	private static boolean parseBooleanOrDefault(String param, boolean defaultValue) {
		return defaultValue ?
				! ("no".equalsIgnoreCase(param) || "false".equalsIgnoreCase(param)) :
				"yes".equalsIgnoreCase(param) || "true".equalsIgnoreCase(param);
	}

	static int completeSettingsFromApplet(final JApplet applet,
                                          ConnectionParams connectionParams,
                                          ProtocolSettings rfbSettings,
                                          UiSettings uiSettings,
                                          Viewer.ApplicationSettings applicationSettings,
                                          Viewer.AppletSettings appletSettings) {
        appletSettings.isSeparateFrame = parseBooleanOrDefault(applet.getParameter(ARG_OPEN_NEW_WINDOW), true);
        final int paramsMask = completeSettings(
                new ParamsRetriever() {
                    @Override
                    public String getParamByName(String name) {
                        return applet.getParameter(name);
                    }
                },
                connectionParams, rfbSettings, uiSettings, applicationSettings);
        appletSettings.allowInteractiveConnections =
                parseBooleanOrDefault(applet.getParameter(ARG_ALLOW_APPLET_INTERACTIVE_CONNECTIONS), false);
        if ( ! appletSettings.allowInteractiveConnections && connectionParams.isHostNameEmpty()) {
            connectionParams.hostName = applet.getCodeBase().getHost();
        }
        return paramsMask;
	}


}
