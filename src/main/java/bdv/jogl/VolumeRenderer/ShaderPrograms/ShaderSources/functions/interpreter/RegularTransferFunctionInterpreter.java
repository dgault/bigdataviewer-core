package bdv.jogl.VolumeRenderer.ShaderPrograms.ShaderSources.functions.interpreter;

import static bdv.jogl.VolumeRenderer.ShaderPrograms.ShaderSources.MultiVolumeRendererShaderSource.*;
/**
 * Desamples a regular sampled transfer function
 * @author michael
 *
 */
public class RegularTransferFunctionInterpreter extends AbstractTransferFunctionInterpreter {
	@Override
	public String[] declaration() {
		String dec[] ={
				"#line "+Thread.currentThread().getStackTrace()[1].getLineNumber() +" 11",
				"",
				"uniform sampler1D "+suvColorTexture+";",
				"",
				"vec4 "+getFunctionName()+"(float vbegin, float vend, float distance){",
				"	float texoffset = 1.0/(2.0*"+suvMaxVolumeValue+");",
				"	float texnorm = ("+suvMaxVolumeValue+"-1.0)/"+suvMaxVolumeValue+";",
				"	vec4 color = texture("+suvColorTexture+",vend*texnorm+texoffset);",
				"	float tau = color.a;",
				"	float alpha = 1.0 - exp(-tau*distance);",
				"	color *= tau;",
				"	color.a = alpha;",
				"	return color;",
				"}",
				""
		};
		return dec;
	}
}
