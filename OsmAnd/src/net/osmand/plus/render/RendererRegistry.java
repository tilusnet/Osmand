package net.osmand.plus.render;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.render.DefaultRenderingRulesStorage;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.render.RenderingRulesStorage.RenderingRulesStorageResolver;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;


public class RendererRegistry {

	private final static Log log = PlatformUtil.getLog(RendererRegistry.class);
	
	public final static String DEFAULT_RENDER = "OsmAnd";  //$NON-NLS-1$
	public final static String DEFAULT_RENDER_FILE_PATH = "default.render.xml";
	public final static String TOURING_VIEW = "Touring view (contrast and details)";  //$NON-NLS-1$
	public final static String WINTER_SKI_RENDER = "Winter and ski";  //$NON-NLS-1$
	public final static String NAUTICAL_RENDER = "Nautical";  //$NON-NLS-1$
	
	private RenderingRulesStorage defaultRender = null;
	private RenderingRulesStorage currentSelectedRender = null;
	
	private Map<String, File> externalRenderers = new LinkedHashMap<String, File>();
	private Map<String, String> internalRenderers = new LinkedHashMap<String, String>();
	
	private Map<String, RenderingRulesStorage> renderers = new LinkedHashMap<String, RenderingRulesStorage>();

    public interface IRendererLoadedEventListener {
        void onRendererLoaded(String name, RenderingRulesStorage rules, InputStream source);
    }

    private IRendererLoadedEventListener rendererLoadedEventListener;

	private OsmandApplication app;
	
	public RendererRegistry(OsmandApplication app){
		this.app = app;
		internalRenderers.put(DEFAULT_RENDER, DEFAULT_RENDER_FILE_PATH);
		internalRenderers.put(TOURING_VIEW, "Touring-view_(more-contrast-and-details)" +".render.xml");
		internalRenderers.put("UniRS", "UniRS" + ".render.xml");
		internalRenderers.put("LightRS", "LightRS" + ".render.xml");
		internalRenderers.put(NAUTICAL_RENDER, "nautical" + ".render.xml");
		internalRenderers.put(WINTER_SKI_RENDER, "skimap" + ".render.xml");
	}
	
	public RenderingRulesStorage defaultRender() {
		if(defaultRender == null){
			defaultRender = getRenderer(DEFAULT_RENDER);
		}
		return defaultRender;
	}

	public RenderingRulesStorage getRenderer(String name) {
		if(renderers.containsKey(name)){
			return renderers.get(name);
		}
		if(!hasRender(name)){
			return null;
		}
		try {
			RenderingRulesStorage r = loadRenderer(name, new LinkedHashMap<String, RenderingRulesStorage>(), new LinkedHashMap<String, String>());
			renderers.put(name, r);
			return r;
		} catch (IOException e) {
			log.error("Error loading renderer", e); //$NON-NLS-1$
		} catch (XmlPullParserException e) {
			log.error("Error loading renderer", e); //$NON-NLS-1$
		}
		return null;
	}

	private boolean hasRender(String name) {
		return externalRenderers.containsKey(name) || internalRenderers.containsKey(name);
	}
	
	private static boolean USE_PRECOMPILED_STYLE = false;
	private RenderingRulesStorage loadRenderer(String name, final Map<String, RenderingRulesStorage> loadedRenderers, 
			final Map<String, String> renderingConstants) throws IOException,  XmlPullParserException {
		if ((name.equals(DEFAULT_RENDER) || name.equalsIgnoreCase("default")) && USE_PRECOMPILED_STYLE) {
			RenderingRulesStorage rrs = new RenderingRulesStorage("", null);
			new DefaultRenderingRulesStorage().createStyle(rrs);
			log.info("INIT rendering from class");
			return rrs;
		}
		InputStream is = getInputStream(name);
		if(is == null) {
			return null;
		}
		try {
			XmlPullParser parser = PlatformUtil.newXMLPullParser();
			parser.setInput(is, "UTF-8");
			int tok;
			while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
				if (tok == XmlPullParser.START_TAG) {
					String tagName = parser.getName();
					if (tagName.equals("renderingConstant")) {
						if (!renderingConstants.containsKey(parser.getAttributeValue("", "name"))) {
							renderingConstants.put(parser.getAttributeValue("", "name"), 
									parser.getAttributeValue("", "value"));
						}
					}
				}
			}
		} finally {
			is.close();
		}

		// parse content
		is = getInputStream(name);
		final RenderingRulesStorage main = new RenderingRulesStorage(name, renderingConstants);
		
		loadedRenderers.put(name, main);
		try {
			main.parseRulesFromXmlInputStream(is, new RenderingRulesStorageResolver() {

				@Override
				public RenderingRulesStorage resolve(String name, RenderingRulesStorageResolver ref) throws XmlPullParserException {
					// reload every time to propogate rendering constants
					if (loadedRenderers.containsKey(name)) {
						log.warn("Circular dependencies found " + name); //$NON-NLS-1$
					}
					RenderingRulesStorage dep = null;
					try {
						dep = loadRenderer(name, loadedRenderers, renderingConstants);
					} catch (IOException e) {
						log.warn("Dependent renderer not found : " + e.getMessage(), e); //$NON-NLS-1$
					}
					if (dep == null) {
						log.warn("Dependent renderer not found : " + name); //$NON-NLS-1$
					}
					return dep;
				}
			});
		} finally {
			is.close();
		}

        if (rendererLoadedEventListener != null)
            rendererLoadedEventListener.onRendererLoaded(name, main, getInputStream(name));

		return main;
	}

	public InputStream getInputStream(String name) throws FileNotFoundException {
		InputStream is;
		if("default".equalsIgnoreCase(name)) {
			name = DEFAULT_RENDER;
		} 
		if(externalRenderers.containsKey(name)){
			is = new FileInputStream(externalRenderers.get(name));
		} else if(internalRenderers.containsKey(name)){
			File fl = getFileForInternalStyle(name);
			if(fl.exists()) {
				is = new FileInputStream(fl);
			} else {
				copyFileForInternalStyle(name);
				is = RenderingRulesStorage.class.getResourceAsStream(internalRenderers.get(name));
			}
		} else {
			throw new IllegalArgumentException("Not found " + name); //$NON-NLS-1$
		}
		return is;
	}

	public void copyFileForInternalStyle(String name) {
		try {
			FileOutputStream fout = new FileOutputStream(getFileForInternalStyle(name));
			Algorithms.streamCopy(RenderingRulesStorage.class.getResourceAsStream(internalRenderers.get(name)),
					fout);
			fout.close();
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
	}
	
	public Map<String, String> getInternalRenderers() {
		return internalRenderers;
	}

	public File getFileForInternalStyle(String name) {
		if(!internalRenderers.containsKey(name)) {
			return new File(app.getAppPath(IndexConstants.RENDERERS_DIR), "style.render.xml");
		}
		File fl = new File(app.getAppPath(IndexConstants.RENDERERS_DIR), internalRenderers.get(name));
		return fl;
	}
	
	public void initRenderers(IProgress progress) {
		File file = app.getAppPath(IndexConstants.RENDERERS_DIR);
		file.mkdirs();
		Map<String, File> externalRenderers = new LinkedHashMap<String, File>(); 
		if (file.exists() && file.canRead()) {
			File[] lf = file.listFiles();
			if (lf != null) {
				for (File f : lf) {
					if (f != null && f.getName().endsWith(IndexConstants.RENDERER_INDEX_EXT)) {
						if(!internalRenderers.containsValue(f.getName())) {
							String name = f.getName().substring(0, f.getName().length() - IndexConstants.RENDERER_INDEX_EXT.length());
							externalRenderers.put(name, f);							
						}
					}
				}
			}
		}
		this.externalRenderers = externalRenderers;
		String r = app.getSettings().RENDERER.get();
		if(r != null){
			RenderingRulesStorage obj = getRenderer(r);
			if(obj != null){
				setCurrentSelectedRender(obj);
			}
		}
	}
	
	public Collection<String> getRendererNames(){
		LinkedHashSet<String> names = new LinkedHashSet<String>();
		names.add(DEFAULT_RENDER);
		names.addAll(internalRenderers.keySet());
		names.addAll(externalRenderers.keySet());
		return names;
	}

	public RenderingRulesStorage getCurrentSelectedRenderer() {
		if(currentSelectedRender == null){
			return defaultRender();
		}
		return currentSelectedRender;
	}
	
	public void setCurrentSelectedRender(RenderingRulesStorage currentSelectedRender) {
		this.currentSelectedRender = currentSelectedRender;
	}

    public void setRendererLoadedEventListener(IRendererLoadedEventListener listener) {
        rendererLoadedEventListener = listener;
    }

    public IRendererLoadedEventListener getRendererLoadedEventListener() {
        return rendererLoadedEventListener;
    }
}
