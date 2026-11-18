package ch.hsr.servicecutter.model.analyzer;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents coupling and cohesion metrics for a service.
 */
public class ServiceMetrics {

	@JsonProperty
	private double cohesion;
	
	@JsonProperty
	private double coupling;
	
	@JsonProperty
	private int numberOfNanoentities;
	
	@JsonProperty
	private int numberOfDependencies;
	
	public ServiceMetrics() {
		// Jackson
	}
	
	public ServiceMetrics(double cohesion, double coupling, int numberOfNanoentities, int numberOfDependencies) {
		this.cohesion = cohesion;
		this.coupling = coupling;
		this.numberOfNanoentities = numberOfNanoentities;
		this.numberOfDependencies = numberOfDependencies;
	}
	
	public double getCohesion() {
		return cohesion;
	}
	
	public void setCohesion(double cohesion) {
		this.cohesion = cohesion;
	}
	
	public double getCoupling() {
		return coupling;
	}
	
	public void setCoupling(double coupling) {
		this.coupling = coupling;
	}
	
	public int getNumberOfNanoentities() {
		return numberOfNanoentities;
	}
	
	public void setNumberOfNanoentities(int numberOfNanoentities) {
		this.numberOfNanoentities = numberOfNanoentities;
	}
	
	public int getNumberOfDependencies() {
		return numberOfDependencies;
	}
	
	public void setNumberOfDependencies(int numberOfDependencies) {
		this.numberOfDependencies = numberOfDependencies;
	}
	
	@Override
	public String toString() {
		return String.format("ServiceMetrics[cohesion=%.2f, coupling=%.2f, nanoentities=%d, dependencies=%d]", 
				cohesion, coupling, numberOfNanoentities, numberOfDependencies);
	}
}
