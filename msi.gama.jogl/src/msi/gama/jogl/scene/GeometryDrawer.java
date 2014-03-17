/**
 * Created by drogoul, 16 mars 2014
 * 
 */
package msi.gama.jogl.scene;

import msi.gama.jogl.utils.*;
import msi.gama.jogl.utils.JTSGeometryOpenGLDrawer.JTSDrawer;
import com.vividsolutions.jts.geom.*;

/**
 * 
 * The class GeometryDrawer.
 * 
 * @author drogoul
 * @since 4 mai 2013
 * 
 */
public class GeometryDrawer extends ObjectDrawer<GeometryObject> {

	JTSDrawer jtsDrawer;

	public GeometryDrawer(final JOGLAWTGLRenderer r) {
		super(r);
		jtsDrawer = new JTSDrawer(r);
	}

	@Override
	protected void _draw(final GeometryObject geometry) {
		switch (geometry.type) {
			case MULTIPOLYGON:
				jtsDrawer.drawMultiPolygon((MultiPolygon) geometry.geometry, geometry.getColor(),
					geometry.getAlpha(), geometry.fill, geometry.border, geometry.isTextured, geometry,
					geometry.height, geometry.rounded, geometry.getZ_fighting_id());
				break;
			case SPHERE:
				jtsDrawer.drawSphere(geometry);
				break;
			case HEMISPHERE:
				jtsDrawer.drawHemiSphere(geometry);
				break;
			case CONE:
				jtsDrawer.drawCone3D(geometry);
				break;
			case TEAPOT:
				jtsDrawer.drawTeapot(geometry);
				break;
			case PYRAMID:
				jtsDrawer.drawPyramid(geometry);
				break;
			case RGBCUBE:
				jtsDrawer.drawRGBCube(geometry);
				break;
			case RGBTRIANGLE:
				jtsDrawer.drawRGBTriangle(geometry);
				break;
			case POLYLINECYLINDER:
				jtsDrawer.DrawMultiLineCylinder(geometry.geometry, geometry.getColor(), geometry.getAlpha(),
					geometry.height);
				break;
			case LINECYLINDER:
				jtsDrawer.drawLineCylinder(geometry.geometry, geometry.getColor(), geometry.getAlpha(),
					geometry.height);
				break;
			case POLYGON:
			case ENVIRONMENT:
			case POLYHEDRON:
			case CUBE:
			case BOX:
			case CYLINDER:
			case GRIDLINE:
				if ( geometry.height > 0 ) {
					jtsDrawer.DrawPolyhedre((Polygon) geometry.geometry, geometry.getColor(), geometry.getAlpha(),
						geometry.fill, geometry.height, true, geometry.border, geometry.isTextured, geometry,
						geometry.rounded, geometry.getZ_fighting_id());
				} else {
					if ( jtsDrawer.renderer.computeNormal ) {
						int norm_dir = 1;
						Vertex[] vertices = jtsDrawer.getExteriorRingVertices((Polygon) geometry.geometry);
						if ( !jtsDrawer.IsClockwise(vertices) ) {
							norm_dir = -1;
						}
						jtsDrawer.DrawPolygon((Polygon) geometry.geometry, geometry.getColor(),
							geometry.getAlpha(), geometry.fill, geometry.border, geometry.isTextured, geometry,
							true, geometry.rounded, geometry.getZ_fighting_id(), norm_dir);
					} else {
						jtsDrawer.DrawPolygon((Polygon) geometry.geometry, geometry.getColor(),
							geometry.getAlpha(), geometry.fill, geometry.border, geometry.isTextured, geometry,
							true, geometry.rounded, geometry.getZ_fighting_id(), -1);
					}

				}
				break;
			case MULTILINESTRING:
				jtsDrawer.DrawMultiLineString((MultiLineString) geometry.geometry, 0, geometry.getColor(),
					geometry.getAlpha(), geometry.height);
				break;
			case LINESTRING:
			case LINEARRING:
			case PLAN:
			case POLYPLAN:
				if ( geometry.height > 0 ) {
					jtsDrawer.drawPlan((LineString) geometry.geometry, 0, geometry.getColor(), geometry.getAlpha(),
						geometry.height, 0, true);
				} else {
					jtsDrawer.drawLineString((LineString) geometry.geometry, 0, 1.2f, geometry.getColor(),
						geometry.getAlpha());
				}
				break;
			case POINT:
				jtsDrawer.DrawPoint((Point) geometry.geometry, 0, 10, renderer.getMaxEnvDim() / 1000,
					geometry.getColor(), geometry.getAlpha());
				break;
		}
	}
}