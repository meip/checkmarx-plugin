package com.checkmarx.jenkins;

import hudson.Functions;
import hudson.PluginWrapper;
import hudson.model.*;
import hudson.model.Messages;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.test.*;
import hudson.util.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.StackedAreaRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.RectangleInsets;
import org.jvnet.localizer.Localizable;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA.
 * User: denis
 * Date: 3/11/13
 * Time: 11:47
 * Description:
 */

// One option is to inherit from AbstractTestResultAction<CxScanResult>


public class CxScanResult implements HealthReportingAction {


    public final AbstractBuild<?,?> owner;

    private Map<String,String> descriptions = new ConcurrentHashMap<String, String>();

    public CxScanResult(AbstractBuild owner)
    {
        this.owner = owner;
    }


    @Override
    public String getIconFileName() {
        return getIconPath() + "CxIcon24x24.png";
    }

    @Override
    public String getDisplayName() {
        return "Checkmarx Scan Results";
    }

    @Override
    public String getUrlName() {
        return "checkmarx";
    }

    public String getIconPath() {
        PluginWrapper wrapper = Hudson.getInstance().getPluginManager().getPlugin(CxPlugin.class);
        return "/plugin/"+ wrapper.getShortName()+"/";
    }

    public int getHighCount()
    {
        return 1;
    }

    public int getMediumCount()
    {
        return 2;
    }

    public int getLowCount()
    {
        return 3;
    }

    public int getInfoCount()
    {
        return 4;
    }

    // Example method, to remove on finish
    public void doFloatingBox(org.kohsuke.stapler.StaplerRequest req,
                      org.kohsuke.stapler.StaplerResponse rsp)
            throws IOException
    {

        hudson.util.Graph graph = new hudson.util.Graph(System.currentTimeMillis(),600,600){

            @Override
            protected JFreeChart createGraph() {
                DefaultCategoryDataset ds = new DefaultCategoryDataset();
                ds.addValue(1,"row1key","col1key");
                ds.addValue(4,"row1key","col2key");
                ds.addValue(2,"row1key","col3key");
                ds.addValue(5,"row1key","col4key");
                JFreeChart j = ChartFactory.createLineChart("Checkmarx Scan Results","Build","# of Vulnerabilities",ds, PlotOrientation.VERTICAL,true,true,true);
                return j;
            }
        };
        graph.doPng(req,rsp);
    }

    // Example method, to remove on finish
    public void doMap(org.kohsuke.stapler.StaplerRequest req,
                      org.kohsuke.stapler.StaplerResponse rsp)
            throws IOException
    {

        hudson.util.Graph graph = new hudson.util.Graph(System.currentTimeMillis(),600,600){

            @Override
            protected JFreeChart createGraph() {
                DefaultCategoryDataset ds = new DefaultCategoryDataset();
                ds.addValue(1,"row1key","col1key");
                ds.addValue(4,"row1key","col2key");
                ds.addValue(2,"row1key","col3key");
                ds.addValue(5,"row1key","col4key");
                JFreeChart j = ChartFactory.createLineChart("Checkmarx Scan Results","Build","# of Vulnerabilities",ds, PlotOrientation.VERTICAL,true,true,true);
                return j;
            }
        };
        graph.doMap(req,rsp);
    }


    /**
     * Gets the diff string of failures.
     */
    public final String getFailureDiffString() {
        CxScanResult prev = getPreviousResult();
        if(prev==null)  return "";  // no record

        return " / "+ Functions.getDiffString(this.getHighCount() - prev.getHighCount());
    }

    public HealthReport getBuildHealth() {
        final int highCount = getHighCount();
        int score = highCount; // TODO: implement real health calculation
        String description = "Number of high severity vulnerabilities: " + Integer.toString(highCount);
        return new HealthReport(score, description);
    }

    /**
     * Gets the test result of the previous build, if it's recorded, or null.
     */

    private CxScanResult getPreviousResult() {
        AbstractBuild<?,?> b = owner;
        while(true) {
            b = b.getPreviousBuild();
            if(b==null)
                return null;
            CxScanResult r = b.getAction(CxScanResult.class);
            if(r!=null)
                return r;
        }
    }



    /**
     * A shortcut for summary.jelly
     *
     * @return List of failed tests from associated test result.
     */
    public List<CaseResult> getFailedTests() {
        return Collections.emptyList();
    }

    /**
     * Generates a PNG image for the test result trend.
     */
    public void doGraph( StaplerRequest req, StaplerResponse rsp) throws IOException {
        if(ChartUtil.awtProblemCause!=null) {
            // not available. send out error message
            rsp.sendRedirect2(req.getContextPath()+"/images/headless.png");
            return;
        }

        if(req.checkIfModified(owner.getTimestamp(),rsp))
            return;

        ChartUtil.generateGraph(req,rsp,createChart(req,buildDataSet(req)),calcDefaultSize());
    }

    /**
     * Generates a clickable map HTML for {@link #doGraph(StaplerRequest, StaplerResponse)}.
     */
    public void doGraphMap( StaplerRequest req, StaplerResponse rsp) throws IOException {
        if(req.checkIfModified(owner.getTimestamp(),rsp))
            return;
        ChartUtil.generateClickableMap(req,rsp,createChart(req,buildDataSet(req)),calcDefaultSize());
    }

    /**
     * Returns a full path down to a test result
     */
    public String getTestResultPath(TestResult it) {
        return getUrlName() + "/" + it.getRelativePathFrom(null);
    }

    /**
     * Determines the default size of the trend graph.
     *
     * This is default because the query parameter can choose arbitrary size.
     * If the screen resolution is too low, use a smaller size.
     */
    private Area calcDefaultSize() {
        Area res = Functions.getScreenResolution();
        if(res!=null && res.width<=800)
            return new Area(250,100);
        else
            return new Area(500,200);
    }

    private CategoryDataset buildDataSet(StaplerRequest req) {
        boolean highOnly = Boolean.valueOf(req.getParameter("highOnly"));

        DataSetBuilder<String,ChartUtil.NumberOnlyBuildLabel> dsb = new DataSetBuilder<String,ChartUtil.NumberOnlyBuildLabel>();

        for( CxScanResult a=this; a!=null; a=a.getPreviousResult() ) {
            dsb.add( a.getHighCount(), "High", new ChartUtil.NumberOnlyBuildLabel(a.owner));
            if(!highOnly) {
                dsb.add( a.getMediumCount(), "Medium", new ChartUtil.NumberOnlyBuildLabel(a.owner));
                dsb.add( a.getLowCount(),"Low", new ChartUtil.NumberOnlyBuildLabel(a.owner));
            }
        }
        return dsb.build();
    }

    private JFreeChart createChart(StaplerRequest req,CategoryDataset dataset) {

        final String relPath = getRelPath(req);

        final JFreeChart chart = ChartFactory.createStackedAreaChart(
                null,                   // chart title
                null,                   // unused
                "count",                  // range axis label
                dataset,                  // data
                PlotOrientation.VERTICAL, // orientation
                false,                     // include legend
                true,                     // tooltips
                false                     // urls
        );

        // NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...

        // set the background color for the chart...

//        final StandardLegend legend = (StandardLegend) chart.getLegend();
//        legend.setAnchor(StandardLegend.SOUTH);

        chart.setBackgroundPaint(Color.white);

        final CategoryPlot plot = chart.getCategoryPlot();

        // plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlinePaint(null);
        plot.setForegroundAlpha(0.8f);
//        plot.setDomainGridlinesVisible(true);
//        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.black);

        CategoryAxis domainAxis = new ShiftedCategoryAxis(null);
        plot.setDomainAxis(domainAxis);
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
        domainAxis.setLowerMargin(0.0);
        domainAxis.setUpperMargin(0.0);
        domainAxis.setCategoryMargin(0.0);

        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        StackedAreaRenderer ar = new StackedAreaRenderer2() {
            @Override
            public String generateURL(CategoryDataset dataset, int row, int column) {
                ChartUtil.NumberOnlyBuildLabel label = (ChartUtil.NumberOnlyBuildLabel) dataset.getColumnKey(column);
                return relPath+label.build.getNumber()+"/testReport/";  //TODO: Check if need to change testReport to checkmarx
            }

            @Override
            public String generateToolTip(CategoryDataset dataset, int row, int column) {
                ChartUtil.NumberOnlyBuildLabel label = (ChartUtil.NumberOnlyBuildLabel) dataset.getColumnKey(column);
                AbstractTestResultAction a = label.build.getAction(AbstractTestResultAction.class);
                switch (row) {
                    case 0:
                        return String.valueOf(hudson.tasks.test.Messages.AbstractTestResultAction_fail(label.build.getDisplayName(), a.getFailCount()));
                    case 1:
                        return String.valueOf(hudson.tasks.test.Messages.AbstractTestResultAction_skip(label.build.getDisplayName(), a.getSkipCount()));
                    default:
                        return String.valueOf(hudson.tasks.test.Messages.AbstractTestResultAction_test(label.build.getDisplayName(), a.getTotalCount()));
                }
            }
        };
        plot.setRenderer(ar);
        ar.setSeriesPaint(0,ColorPalette.RED); // Failures.
        ar.setSeriesPaint(1,ColorPalette.YELLOW); // Skips.
        ar.setSeriesPaint(2,ColorPalette.BLUE); // Total.

        // crop extra space around the graph
        plot.setInsets(new RectangleInsets(0,0,0,5.0));

        return chart;
    }

    private String getRelPath(StaplerRequest req) {
        String relPath = req.getParameter("rel");
        if(relPath==null)   return "";
        return relPath;
    }

    /**
     * {@link TestObject}s do not have their own persistence mechanism, so updatable data of {@link TestObject}s
     * need to be persisted by the owning {@link AbstractTestResultAction}, and this method and
     * {@link #setDescription(TestObject, String)} provides that logic.
     *
     * <p>
     * The default implementation stores information in the 'this' object.
     *
     * @see TestObject#getDescription()
     */
    protected String getDescription(TestObject object) {
        return descriptions.get(object.getId());
    }

    protected void setDescription(TestObject object, String description) {
        descriptions.put(object.getId(), description);
    }

    public Object readResolve() {
        if (descriptions == null) {
            descriptions = new ConcurrentHashMap<String, String>();
        }

        return this;
    }

}