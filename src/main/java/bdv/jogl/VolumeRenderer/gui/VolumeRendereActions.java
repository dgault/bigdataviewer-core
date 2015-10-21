package bdv.jogl.VolumeRenderer.gui;

import java.awt.event.ActionEvent;

import bdv.jogl.VolumeRenderer.Scene.InteraktionAnimator;
import bdv.jogl.VolumeRenderer.gui.GLWindow.GLWindow;
import bdv.util.AbstractNamedAction;

/**
 * Defines all menu actions
 * @author michael
 *
 */
public class VolumeRendereActions {

	public static class OpenVolumeRendererAction extends AbstractNamedAction{

		/**
		 * default version
		 */
		private static final long serialVersionUID = 1L;

		private final GLWindow window3D;
		
		private final SceneControlsWindow controls;
		
		private final InteraktionAnimator animator;
		
		public OpenVolumeRendererAction(final String name, final GLWindow win3d, SceneControlsWindow controls, InteraktionAnimator animator ) {
			super(name);
			window3D = win3d;
			this.controls = controls;
			this.animator = animator;
		}
		
		
		@Override
		public void actionPerformed(ActionEvent e) {
			animator.startInitAnimation();
			controls.setVisible(true);
			window3D.setVisible(true);
		}
	}
	
}