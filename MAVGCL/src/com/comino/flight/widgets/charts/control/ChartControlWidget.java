/****************************************************************************
 *
 *   Copyright (c) 2016 Eike Mansfeld ecm@gmx.de. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 ****************************************************************************/

package com.comino.flight.widgets.charts.control;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

import com.comino.flight.log.FileHandler;
import com.comino.flight.model.AnalysisDataModelMetaData;
import com.comino.flight.model.service.AnalysisModelService;
import com.comino.flight.observables.StateProperties;
import com.comino.flight.widgets.status.StatusWidget;
import com.comino.flight.widgets.statusline.StatusLineWidget;
import com.comino.mav.control.IMAVController;
import com.comino.msp.main.control.listener.IMSPModeChangedListener;
import com.comino.msp.model.collector.ModelCollectorService;
import com.comino.msp.model.segment.Status;
import com.comino.msp.utils.ExecutorService;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

public class ChartControlWidget extends Pane implements IMSPModeChangedListener {

	private static final int TRIG_ARMED 		= 0;
	private static final int TRIG_LANDED		= 1;
	private static final int TRIG_ALTHOLD		= 2;
	private static final int TRIG_POSHOLD 		= 3;

	private static final String[]  TRIG_START_OPTIONS = { "armed", "started", "altHold entered", "posHold entered" };
	private static final String[]  TRIG_STOP_OPTIONS = { "unarmed", "landed", "altHold left", "posHold left" };

	private static final Integer[] TRIG_DELAY_OPTIONS = { 0, 2, 5, 10, 30 };
	private static final Integer[] TOTAL_TIME = { 10, 30, 60, 240, 1200 };

	@FXML
	private ToggleButton recording;

	@FXML
	private Button clear;

	@FXML
	private CheckBox enablemodetrig;

	@FXML
	private ChoiceBox<String> trigstart;

	@FXML
	private ChoiceBox<String> trigstop;

	@FXML
	private ChoiceBox<Integer> trigdelay;

	@FXML
	private ChoiceBox<Integer> predelay;

	@FXML
	private ChoiceBox<Integer> totaltime;

	@FXML
	private ComboBox<String> keyfigures;

	@FXML Slider scroll;

	@FXML
	private Circle isrecording;


	private Task<Integer> task;

	private IMAVController control;
	private List<IChartControl> charts = null;

	private int triggerStartMode =0;
	private int triggerStopMode  =0;
	private int triggerDelay =0;

	private boolean modetrigger  = false;
	protected int totalTime_sec = 30;
	private AnalysisModelService modelService;

	private long scroll_tms = 0;


	public ChartControlWidget() {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ChartControlWidget.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		try {
			fxmlLoader.load();
		} catch (IOException exception) {

			throw new RuntimeException(exception);
		}
		charts = new ArrayList<IChartControl>();

		task = new Task<Integer>() {

			@Override
			protected Integer call() throws Exception {
				while(true) {
					LockSupport.parkNanos(500000000L);
					if(isDisabled()) {
						continue;
					}

					if (isCancelled()) {
						break;
					}

					updateValue(modelService.getMode());
				}
				return modelService.getMode();
			}
		};

		task.valueProperty().addListener(new ChangeListener<Integer>() {

			@Override
			public void changed(ObservableValue<? extends Integer> observableValue, Integer oldData, Integer newData) {
				switch(newData) {
				case ModelCollectorService.STOPPED:
					clear.setDisable(false);
					StatusLineWidget.showProgressIndicator(false);
					recording.selectedProperty().set(false);
					isrecording.setFill(Color.LIGHTGREY); break;
				case ModelCollectorService.PRE_COLLECTING:
					clear.setDisable(true);
					FileHandler.getInstance().clear();
					recording.selectedProperty().set(true);
					isrecording.setFill(Color.LIGHTBLUE); break;
				case ModelCollectorService.POST_COLLECTING:
					clear.setDisable(true);
					recording.selectedProperty().set(true);
					isrecording.setFill(Color.LIGHTYELLOW); break;
				case ModelCollectorService.COLLECTING:
					clear.setDisable(true);
					FileHandler.getInstance().clear();
					StatusLineWidget.showProgressIndicator(true);
					recording.selectedProperty().set(true);
					isrecording.setFill(Color.RED); break;
				}
			}
		});

	}

	@FXML
	private void initialize() {


		trigstart.getItems().addAll(TRIG_START_OPTIONS);
		trigstart.getSelectionModel().select(0);
		trigstart.setDisable(true);
		trigstop.getItems().addAll(TRIG_STOP_OPTIONS);
		trigstop.getSelectionModel().select(0);
		trigstop.setDisable(true);
		trigdelay.getItems().addAll(TRIG_DELAY_OPTIONS);
		trigdelay.getSelectionModel().select(0);
		trigdelay.setDisable(true);
		predelay.getItems().addAll(TRIG_DELAY_OPTIONS);
		predelay.getSelectionModel().select(0);
		predelay.setDisable(true);

		totaltime.getItems().addAll(TOTAL_TIME);
		totaltime.getSelectionModel().select(1);

		buildKeyfigureModelSelection();

		recording.disableProperty().bind(StateProperties.getInstance().getConnectedProperty().not());

		recording.setOnMousePressed(event -> {
			enablemodetrig.selectedProperty().set(false);
		});

		recording.selectedProperty().addListener((observable, oldvalue, newvalue) -> {
			recording(newvalue, 0);
			if(!newvalue.booleanValue())
				scroll.setValue(1);
		});

		clear.setOnAction((ActionEvent event)-> {
			FileHandler.getInstance().clear();
			scroll.setValue(0);
			scroll.setDisable(true);
			modelService.clearModelList();
			for(IChartControl chart : charts) {
				if(chart.getScrollProperty()!=null)
					chart.getScrollProperty().set(1);
				chart.refreshChart();
			}
		});

		recording.setTooltip(new Tooltip("start/stop recording"));


		enablemodetrig.selectedProperty().addListener((observable, oldvalue, newvalue) -> {
			modetrigger = newvalue;
			trigdelay.setDisable(oldvalue);
			trigstop.setDisable(oldvalue);
			trigstart.setDisable(oldvalue);
		});

		trigstart.getSelectionModel().selectedIndexProperty().addListener((observable, oldvalue, newvalue) -> {
			triggerStartMode = newvalue.intValue();
			triggerStopMode  = newvalue.intValue();
			trigstop.getSelectionModel().select(triggerStopMode);
		});

		trigstop.getSelectionModel().selectedIndexProperty().addListener((observable, oldvalue, newvalue) -> {
			triggerStopMode = newvalue.intValue();
		});


		trigdelay.getSelectionModel().selectedItemProperty().addListener((observable, oldvalue, newvalue) -> {
			triggerDelay = newvalue.intValue();
		});

		totaltime.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			totalTime_sec  = newValue.intValue();
			modelService.setTotalTimeSec(totalTime_sec);

			for(IChartControl chart : charts) {
				if(chart.getTimeFrameProperty()!=null)
					chart.getTimeFrameProperty().set(newValue.intValue());
				if(chart.getScrollProperty()!=null)
					chart.getScrollProperty().set(1);
			}

			if(modelService.getModelList().size() < totalTime_sec * 1000 /  modelService.getCollectorInterval_ms() || modelService.isCollecting())
				scroll.setDisable(true);
			else
				scroll.setDisable(false);
			scroll.setValue(1);
		});


		scroll.valueProperty().addListener((observable, oldvalue, newvalue) -> {
			if((System.currentTimeMillis() - scroll_tms)>20) {
				scroll_tms = System.currentTimeMillis();
				for(IChartControl chart : charts) {
					if(chart.getScrollProperty()!=null)
						chart.getScrollProperty().set(1f-newvalue.floatValue()/1000);
				}
			}
		});

		scroll.setDisable(true);
		scroll.setOnMouseClicked(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent click) {
				if (click.getClickCount() == 2) {
					scroll.setValue(1000);
					for(IChartControl chart : charts) {
						if(chart.getScrollProperty()!=null)
							chart.getScrollProperty().set(0);
					}
				}
			}
		});


		StateProperties.getInstance().getConnectedProperty().addListener((observable, oldvalue, newvalue) -> {
			if(!newvalue.booleanValue())
				recording(false, 0);
		});


		enablemodetrig.selectedProperty().set(true);


	}


	public void setup(IMAVController control, StatusWidget statuswidget) {
		this.control = control;
		this.modelService =  AnalysisModelService.getInstance(control.getCurrentModel());
		this.control.addModeChangeListener(this);
		this.modelService.setTotalTimeSec(totalTime_sec);
		this.modelService.clearModelList();

		StateProperties.getInstance().getRecordingProperty().bind(recording.selectedProperty());

		ExecutorService.get().execute(task);

		for(IChartControl chart : charts) {
			if(chart.getTimeFrameProperty()!=null)
				chart.getTimeFrameProperty().set(30);
			if(chart.getScrollProperty()!=null)
				chart.getScrollProperty().set(1);
		}

	}


	public void addChart(IChartControl chart) {
		charts.add(chart);
	}

	public void refreshCharts() {

		for(IChartControl chart : charts) {
			if(chart.getScrollProperty()!=null)
				chart.getScrollProperty().set(1);
			chart.refreshChart();
		}

		if(modelService.getModelList().size() > totalTime_sec * 1000 /  modelService.getCollectorInterval_ms())
			scroll.setDisable(false);
	}

	@Override
	public void update(Status oldStat, Status newStat) {

		if(!modetrigger)
			return;

		if(!control.getCollector().isCollecting()) {
			switch(triggerStartMode) {
			case TRIG_ARMED: 		recording(newStat.isStatus(Status.MSP_ARMED),0); break;
			case TRIG_LANDED:		recording(!newStat.isStatus(Status.MSP_LANDED),0); break;
			case TRIG_ALTHOLD:		recording(newStat.isStatus(Status.MSP_MODE_ALTITUDE)
					&& !newStat.isStatus(Status.MSP_LANDED),0); break;
			case TRIG_POSHOLD:	    recording(newStat.isStatus(Status.MSP_MODE_POSITION)
					&& !newStat.isStatus(Status.MSP_LANDED),0); break;
			}
		} else {
			switch(triggerStopMode) {
			case TRIG_ARMED: 		recording(newStat.isStatus(Status.MSP_ARMED),triggerDelay);
			break;
			case TRIG_LANDED:		recording(!newStat.isStatus(Status.MSP_LANDED),triggerDelay);
			break;
			case TRIG_ALTHOLD:		recording((newStat.isStatus(Status.MSP_MODE_ALTITUDE)
					| newStat.isStatus(Status.MSP_MODE_POSITION))
					&& !newStat.isStatus(Status.MSP_LANDED),triggerDelay);
			break;
			case TRIG_POSHOLD:	    recording(newStat.isStatus(Status.MSP_MODE_POSITION)
					&& !newStat.isStatus(Status.MSP_LANDED),triggerDelay);
			break;
			}
		}
	}


	private void recording(boolean start, int delay) {

		if(start) {
			modelService.start();
			scroll.setDisable(true);
		}
		else {
			modelService.stop(delay);
			if(modelService.getModelList().size() > totalTime_sec * 1000 / modelService.getCollectorInterval_ms()) {
				scroll.setDisable(false);
			}
		}

		for(IChartControl chart : charts) {
			if(chart.getScrollProperty()!=null)
				chart.getScrollProperty().set(1);
		}
	}

	private void buildKeyfigureModelSelection() {

		final AnalysisDataModelMetaData meta = AnalysisDataModelMetaData.getInstance();

		keyfigures.getItems().add("Built-In model definition");
		keyfigures.getItems().add("Custom model definition...");
		keyfigures.getEditor().setText(meta.getDescription());
		keyfigures.setEditable(true);
		keyfigures.getEditor().setEditable(false);


		keyfigures.getSelectionModel().selectedIndexProperty().addListener((o,ov,nv) -> {
			switch(nv.intValue()) {
			case 0: meta.loadModelMetaData(null);
			    break;
			case 1:
				try {
					FileChooser metaFile = new FileChooser();
					metaFile.getExtensionFilters().addAll(new ExtensionFilter("Custom KeyFigure Definition File..", "*.xml"));
					File f = metaFile.showOpenDialog(ChartControlWidget.this.getScene().getWindow());
					if(f!=null) {
						meta.loadModelMetaData(new FileInputStream(f));
					}
					Platform.runLater(() -> {
						keyfigures.getEditor().setText(meta.getDescription());
					});
				} catch(Exception e) {
					Platform.runLater(() -> {
						keyfigures.getSelectionModel().select(0);
					});
				}
				break;
			}

		});

	}

}
