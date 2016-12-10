package bdv.tools.bookmarks.bookmark;

import java.util.TreeSet;

import org.jdom2.Element;

import bdv.viewer.animate.SimilarityTransformAnimator;
import mpicbg.spim.data.XmlHelpers;
import net.imglib2.realtransform.AffineTransform3D;

public class DynamicBookmark implements IBookmark {

	public static final String XML_ELEM_BOOKMARK_NAME = "DynamicBookmark";
	public static final String XML_ELEM_KEY_NAME = "key";
	public static final String XML_ELEM_KEYFRAMES_NAME = "keyframes";
	public static final String XML_ELEM_KEYFRAME_NAME = "keyframe";
	public static final String XML_ELEM_TIMEPOINT_NAME = "timepoint";
	public static final String XML_ELEM_TRANSFORM_NAME = "transform";

	private final String key;

	private final TreeSet<KeyFrame> keyframes;

	public DynamicBookmark(final String key) {
		this.key = key;
		this.keyframes = new TreeSet<>(new KeyFrameComparator());
	}

	public DynamicBookmark(Element element) {
		this.key = restoreKeyFromXml(element);
		this.keyframes = new TreeSet<>(new KeyFrameComparator());

		restoreKeyframesFromXml(element);
	}

	@Override
	public String getKey() {
		return this.key;
	}

	public void add(final KeyFrame keyframe) {
		keyframes.remove(keyframe);
		keyframes.add(keyframe);
	}

	@Override
	public Element toXmlNode() {
		final Element elemBookmark = new Element(XML_ELEM_BOOKMARK_NAME);
		elemBookmark.addContent(XmlHelpers.textElement(XML_ELEM_KEY_NAME, this.key));

		final Element elemKeyframes = new Element(XML_ELEM_KEYFRAMES_NAME);

		for (final KeyFrame keyframe : keyframes) {
			final Element elemKeyframe = new Element(XML_ELEM_KEYFRAME_NAME);
			elemKeyframe.addContent(XmlHelpers.intElement(XML_ELEM_TIMEPOINT_NAME, keyframe.getTimepoint()));
			elemKeyframe
					.addContent(XmlHelpers.affineTransform3DElement(XML_ELEM_TRANSFORM_NAME, keyframe.getTransform()));
			elemKeyframes.addContent(elemKeyframe);
		}

		elemBookmark.addContent(elemKeyframes);

		return elemBookmark;
	}

	private String restoreKeyFromXml(final Element parent) {
		return XmlHelpers.getText(parent, XML_ELEM_KEY_NAME);
	}

	private void restoreKeyframesFromXml(final Element parent) {
		final Element elemKeyframes = parent.getChild(XML_ELEM_KEYFRAMES_NAME);

		for (final Element elemKeyframe : elemKeyframes.getChildren(XML_ELEM_KEYFRAME_NAME)) {
			final int timepoint = XmlHelpers.getInt(elemKeyframe, XML_ELEM_TIMEPOINT_NAME);
			final AffineTransform3D transform = XmlHelpers.getAffineTransform3D(elemKeyframe, XML_ELEM_TRANSFORM_NAME);
			add(new KeyFrame(timepoint, transform));
		}
	}

	/**
	 * Returns the greatest keyframe by timepoint less than to the
	 * given timepoint, or null if there is no such timepoint.
	 * 
	 * @param timepoint
	 *            the reference value
	 * @return previous keyframe by timepoint which is less the given
	 *         timepoint or null
	 */
	public KeyFrame getPreviousKeyFrame(final int timepoint) {
		KeyFrame k = new KeyFrame(timepoint, null);
		return keyframes.lower(k);
	}
	
	/**
	 * Returns the greatest keyframe by timepoint less than or equal to the
	 * given timepoint, or null if there is no such timepoint.
	 * 
	 * @param timepoint
	 *            the reference value
	 * @return previous keyframe by timepoint which is less or equals the given
	 *         timepoint or null
	 */
	public KeyFrame getPreviousOrEqualKeyFrame(final int timepoint) {
		KeyFrame k = new KeyFrame(timepoint, null);
		return keyframes.floor(k);
	}

	/**
	 * Returns the least keyframe by timepoint strictly greater than the given
	 * timepoint, or null if there is no such timepoint.
	 * 
	 * @param timepoint
	 *            the reference value
	 * @return next keyframe by timepoint which is greater than the given
	 *         timepoint or null
	 */
	public KeyFrame getNextKeyFrame(int timepoint) {
		KeyFrame k = new KeyFrame(timepoint, null);
		return keyframes.higher(k);
	}

	/**
	 * Returns interpolated transform between {@link #getPreviousTransform(int)
	 * previous} and {@link #getNextTransform(int) next} transform for given
	 * timepoint.
	 * 
	 * @param timepoint
	 * @param cX
	 * @param cY
	 * @return
	 */
	public AffineTransform3D getInterpolatedTransform(final int timepoint, final double cX, final double cY) {

		if (keyframes.size() < 1) {
			return null;
		}

		KeyFrame previousKeyframe = getPreviousOrEqualKeyFrame(timepoint);
		final KeyFrame nextKeyframe = getNextKeyFrame(timepoint);

		if (previousKeyframe == null) {
			previousKeyframe = keyframes.first();
		}

		if (nextKeyframe == null) {
			AffineTransform3D transform = previousKeyframe.getTransform();
			return transform;
		}
		int animatorTimepoint = Math.min(timepoint, previousKeyframe.getTimepoint());

		final SimilarityTransformAnimator transAnimator = new SimilarityTransformAnimator(
				previousKeyframe.getTransform(), nextKeyframe.getTransform(), cX, cY,
				nextKeyframe.getTimepoint() - animatorTimepoint);
		transAnimator.setTime(0);

		final AffineTransform3D targetTransform = transAnimator.getCurrent(timepoint - animatorTimepoint);

		targetTransform.set(targetTransform.get(0, 3) - cX, 0, 3);
		targetTransform.set(targetTransform.get(1, 3) - cY, 1, 3);

		return targetTransform;
	}
}
