package entity;

public class ClassifierAnalysis {
	
	private String projName;
	
	private int numTrainingRelease;
	
	//# of instances in the training and testing dataset, used to evaluate
	//the percentage of training dataset
	private int numTrainingInstances;
	private int numTestingInstances;
	
	//# of buggy and not buggy instances in training and testing dataset,
	//used to evaluate percentages
	private double numDefectiveTraining;
	private double numNotDefectiveTraining;
	
	private double numDefectiveTesting;
	private double numNotDefectiveTesting;
	
	private int truePositive;
	private int falsePositive;
	private int trueNegative;
	private int falseNegative;
	
	private String classifierName;
	
	private String balancing = "None";
	
	private String featureSelection = "None";
	
	private double precision;
	
	private double recall;
	
	private double rocArea;
	
	private double kappa;
	
	public ClassifierAnalysis(String projName, String classifier) {
		this.projName = projName;
		this.classifierName = classifier;
	}
	
	public String getProjName() {
		return this.projName;
	}
	
	public int setNumTrainingReleases(int numTrainingRelease) {
		return this.numTrainingRelease = numTrainingRelease;
	}
	
	public int getNumTrainingReleases() {
		return this.numTrainingRelease;
	}
	
	public void setNumTrainingInstances(int numTrainingInstances) {
		this.numTrainingInstances = numTrainingInstances;
	}

	public void setNumTestingInstances(int numTestingInstances) {
		this.numTestingInstances = numTestingInstances;
	}
	
	public double getPercentageTraining() {
		return (double)this.numTrainingInstances/(this.numTrainingInstances+this.numTestingInstances);
	}

	public void setNumDefectiveTraining(double numDefectiveTraining) {
		this.numDefectiveTraining = numDefectiveTraining;
	}

	public void setNumNotDefectiveTraining(double numNotDefectiveTraining) {
		this.numNotDefectiveTraining = numNotDefectiveTraining;
	}
	
	public double getPercDefectiveInTraining() {
		return (double)this.numDefectiveTraining/(this.numDefectiveTraining+this.numNotDefectiveTraining);
	}

	public void setNumDefectiveTesting(double numDefectiveTesting) {
		this.numDefectiveTesting = numDefectiveTesting;
	}

	public void setNumNotDefectiveTesting(double numNotDefectiveTesting) {
		this.numNotDefectiveTesting = numNotDefectiveTesting;
	}
	
	public double getPercDefectiveInTesting() {
		return (double)this.numDefectiveTesting/(this.numDefectiveTesting+this.numNotDefectiveTesting);
	}
	
	public String getClassifierName() {
		return this.classifierName;
	}

	public double getPrecision() {
		return precision;
	}

	public void setPrecision(double precision) {
		this.precision = precision;
	}

	public double getRecall() {
		return recall;
	}

	public void setRecall(double recall) {
		this.recall = recall;
	}

	public double getRocArea() {
		return rocArea;
	}

	public void setRocArea(double rocArea) {
		this.rocArea = rocArea;
	}

	public double getKappa() {
		return kappa;
	}

	public void setKappa(double kappa) {
		this.kappa = kappa;
	}

	public String getBalancing() {
		return balancing;
	}

	public void setBalancing(String balancing) {
		this.balancing = balancing;
	}

	public String getFeatureSelection() {
		return featureSelection;
	}

	public void setFeatureSelection(String featureSelection) {
		this.featureSelection = featureSelection;
	}

	public int getTruePositive() {
		return truePositive;
	}

	public void setTruePositive(int truePositive) {
		this.truePositive = truePositive;
	}

	public int getFalsePositive() {
		return falsePositive;
	}

	public void setFalsePositive(int falsePositive) {
		this.falsePositive = falsePositive;
	}

	public int getTrueNegative() {
		return trueNegative;
	}

	public void setTrueNegative(int trueNegative) {
		this.trueNegative = trueNegative;
	}

	public int getFalseNegative() {
		return falseNegative;
	}

	public void setFalseNegative(int falseNegative) {
		this.falseNegative = falseNegative;
	}

}
