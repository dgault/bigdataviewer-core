package bdv.jogl.VolumeRenderer;

import java.util.LinkedList;
import java.util.List;

import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.Matrix4;
import com.jogamp.opengl.math.VectorUtil;

/**
 * Class to calculate the camera matrices for opengl
 * @author michael
 *
 */
public class Camera {

	private List<CameraListener> cameraListeners = new LinkedList<CameraListener>();
	
	private Matrix4 projectionMatrix  = new Matrix4();

	private Matrix4 viewMatrix = new Matrix4();

	public static final int minSize = 1;

	public static final float minZ = 0.0001f;

	public static final float minAlpha = 1f;

	public static final float maxAlpha = 180f;

	public static final float[] defaultUpVector = {0,1,0};

	private Integer height = minSize;

	private Integer width = minSize;

	private float alpha = 45;

	private float znear = 0.1f;

	private float zfar = 1000;

	private float[] eyePoint = {0,1,1};

	private float[] lookAtPoint =  {0,0,0};

	private float[] upVector = defaultUpVector;


	/**
	 * standard constructor
	 */
	public Camera(){

	}

	/**
	 * @return the projectionMatix
	 */
	public final Matrix4 getProjectionMatix() {
		Matrix4 copy = new Matrix4();
		copy.loadIdentity();
		copy.multMatrix(projectionMatrix);
		return copy;
	}

	/**
	 * @return the height
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * @param height the height to set
	 */
	public void setHeight(int height) {
		if(height < minSize){
			this.height = minSize;
			return;
		}

		this.height = height;
	}

	/**
	 * @return the width
	 */
	public int getWidth() {

		return width;
	}

	/**
	 * @param width the width to set
	 */
	public void setWidth(int width) {
		if(width < minSize){
			this.width = minSize;
			return;
		}
		this.width = width;
	}

	/**
	 * @return the alpha
	 */
	public float getAlpha() {
		return alpha;
	}

	/**
	 * @param alpha the alpha to set
	 */
	public void setAlpha(float alpha) {
		if(alpha < minAlpha){
			alpha = minAlpha;
		}

		if(alpha > maxAlpha ){
			alpha = maxAlpha;
		}

		this.alpha = alpha;
	}

	/**
	 * @return the znear
	 */
	public float getZnear() {
		return znear;
	}

	/**
	 * @param znear the znear to set
	 */
	public void setZnear(float znear) {
		if(znear < minZ){
			this.znear = minZ;

		}else{
			this.znear = znear;
		}


		// inconsistent view volume
		if(getZnear() > getZfar()){
			setZfar(getZnear()+minZ);
		}
	}

	/**
	 * @return the zfar
	 */
	public float getZfar() {
		return zfar;
	}

	/**
	 * @param zfar the zfar to set
	 */
	public void setZfar(float zfar) {
		if(!(zfar >getZnear())){

			//increase z by min
			this.zfar=getZnear()+minZ;
			return;
		}
		this.zfar = zfar;
	}

	/**
	 * @return the viewMatrix
	 */
	public final Matrix4 getViewMatrix() {
		Matrix4 copy = new Matrix4();
		copy.loadIdentity(); 
		copy.multMatrix(viewMatrix);
		return copy;
	}

	/**
	 * @return the eyePoint
	 */
	public final float[] getEyePoint() {
		return eyePoint.clone();
	}

	/**
	 * @param eyePoint the eyePoint to set
	 */
	public void setEyePoint(final float[] eyePoint) {
		if(VectorUtil.isVec3Equal(getLookAtPoint(), 0, eyePoint, 0)){
			throw new IllegalArgumentException("Eye must not be equals to look at point!");
		}
		this.eyePoint = eyePoint.clone();
	}

	/**
	 * @return the lookAtPoint
	 */
	public  float[] getLookAtPoint() {
		return lookAtPoint.clone();
	}

	/**
	 * @param lookAtPoint the lookAtPoint to set
	 */
	public void setLookAtPoint(final float[] lookAtPoint) {
		if(VectorUtil.isVec3Equal(getEyePoint(), 0, lookAtPoint, 0)){
			throw new IllegalArgumentException("Look at point must not be equals to eye!");
		}
		this.lookAtPoint = lookAtPoint.clone();
	}

	/**
	 * @return the upVector
	 */
	public final float[] getUpVector() {
		return upVector.clone();
	}

	/**
	 * @param upVector the upVector to set
	 */
	public void setUpVector(final float[] upVector) {
		if(VectorUtil.isVec3Zero(upVector, 0)){
			this.upVector=defaultUpVector.clone();
		}else{
			this.upVector = upVector.clone();
		}

		this.upVector = VectorUtil.normalizeVec3( this.upVector);
	}

	/**
	 * does updatePerspectiveMatrix and updateViewMatrix  
	 */
	public void update(){
		updatePerspectiveMatrix();

		updateViewMatrix();
	}

	/**
	 * update the view matrix of the camera with the current data
	 */
	public void updateViewMatrix(){

		updateLookAt();
		
		//notify listeners
		for(CameraListener l : cameraListeners){
			l.viewMatrixUpdate(getViewMatrix());
		}
	}

	/**
	 * update the angle based perspective
	 */
	public void updatePerspectiveMatrix() {
		
		FloatUtil.makePerspective(projectionMatrix.getMatrix(), 0, true, 
				alpha,width.floatValue()/height.floatValue(), znear, zfar);
		
		//notify listeners
		for(CameraListener l : cameraListeners){
			l.projectionMatrixUpdate(getProjectionMatix());
		}
	}

	
	/**
	 * Adds a new camera listener.
	 * @param listener
	 * @return true if add was successful
	 */
	public boolean addCameraListener(CameraListener listener){
		//wrong value
		if(listener == null){
			return false;
		}
			
		return cameraListeners.add(listener);
	}

	/**
	 * removes all camera listeners if this camera.
	 */
	public void clearCameraListeners(){
		cameraListeners.clear();
	}
	
	/**
	 * updates viewMatrix like glulookat
	 */
	private void updateLookAt(){
		
		float[] mat4Tmp = new float[16];
		FloatUtil.makeLookAt(viewMatrix.getMatrix(), 0, 
				eyePoint, 0,
				lookAtPoint, 0, 
				upVector, 0, mat4Tmp);

	};

}

