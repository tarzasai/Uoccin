package net.ggelardi.uoccin.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Xml;

/**
 * 
 * @author Giorgio.Gelardi
 *
 */
public class TNT {
	private static final String ns = null;
	
	private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
		if (parser.getEventType() != XmlPullParser.START_TAG)
			throw new IllegalStateException();
		int depth = 1;
		while (depth != 0)
			switch (parser.next()) {
				case XmlPullParser.END_TAG:
					depth--;
					break;
				case XmlPullParser.START_TAG:
					depth++;
					break;
			}
	}
	
	private String readLink(XmlPullParser parser) throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, ns, "item");
		String link = null;
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG)
				continue;
			if (parser.getName().equals("link") && parser.next() == XmlPullParser.TEXT)
				link = parser.getText().replace("&amp;", "&");
			else
				skip(parser);
		}
		return link;
	}
	
	private List<String> readFeed(XmlPullParser parser) throws XmlPullParserException, IOException {
		List<String> links = new ArrayList<String>();
		parser.require(XmlPullParser.START_TAG, ns, "rss");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG)
				continue;
			if (parser.getName().equals("item"))
				links.add(readLink(parser));
			else
				skip(parser);
		}
		return links;
	}
	
	public List<String> getLinks() throws IOException, XmlPullParserException {
		URL url = new URL("http://thetvdb.com/rss/newtoday.php");
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setReadTimeout(15000 /* milliseconds */);
		conn.setConnectTimeout(30000 /* milliseconds */);
		conn.setRequestMethod("GET");
		conn.setDoInput(true);
		// Starts the query
		conn.connect();
		InputStream stream = conn.getInputStream();
		try {
			XmlPullParser parser = Xml.newPullParser();
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			parser.setInput(stream, null);
			parser.nextTag();
			return readFeed(parser);
		} finally {
			stream.close();
		}
	}
}