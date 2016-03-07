/*
 * Copyright (c) 2016 by E.Mansfeld
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.comino.flight.widgets.charts.xy;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import com.comino.flight.widgets.charts.control.IChartControl;
import com.comino.mav.control.IMAVController;
import com.comino.model.types.MSTYPE;
import com.comino.msp.model.DataModel;
import com.comino.msp.utils.ExecutorService;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;


public class XYChartWidget extends BorderPane implements IChartControl {


	private static MSTYPE[][] PRESETS = {
			{ MSTYPE.MSP_NONE,			MSTYPE.MSP_NONE							},
			{ MSTYPE.MSP_RNEDX, 		MSTYPE.MSP_RNEDY,		},
			{ MSTYPE.MSP_NEDVX, 		MSTYPE.MSP_NEDVY,		},
			{ MSTYPE.MSP_LERRX, 		MSTYPE.MSP_LERRY,		},
			{ MSTYPE.MSP_GLOBRELX,      MSTYPE.MSP_GLOBRELY, },
			{ MSTYPE.MSP_GLOBRELVX,     MSTYPE.MSP_GLOBRELVY, },
			{ MSTYPE.MSP_ANGLEX, 		MSTYPE.MSP_ANGLEY,	},
			{ MSTYPE.MSP_ACCX, 			MSTYPE.MSP_ACCY, 		},
			{ MSTYPE.MSP_GYROX, 		MSTYPE.MSP_GYROY, 	},
			{ MSTYPE.MSP_RAW_FLOWX, 	MSTYPE.MSP_RAW_FLOWY, },
			{ MSTYPE.MSP_MAGX, 			MSTYPE.MSP_MAGY, },
	};

	private final static String[] PRESET_NAMES = {
			"None",
			"Loc.Pos.rel.",
			"Loc.Speed",
			"Loc.Pos.Error",
			"Glob.Pos.rel",
			"Glob.Speed",
			"Angle",
			"Raw Accelerator",
			"Raw Gyroskope",
			"Raw Flow",
			"Magnetic Field XY"
	};

	private final static String[] SCALES = {
			"Auto", "0.5","1", "2", "5", "10", "50", "100"
	};


	private static int COLLECTOR_CYCLE = 50;

	@FXML
	private LineChart<Number,Number> linechart;

	@FXML
	private NumberAxis xAxis;

	@FXML
	private NumberAxis yAxis;

	@FXML
	private ChoiceBox<String> cseries1;

	@FXML
	private ChoiceBox<String> cseries2;

	@FXML
	private ChoiceBox<String> cseries1_x;

	@FXML
	private ChoiceBox<String> cseries1_y;

	@FXML
	private ChoiceBox<String> cseries2_x;

	@FXML
	private ChoiceBox<String> cseries2_y;

	@FXML
	private ChoiceBox<String> scale;

	@FXML
	private CheckBox normalize;

	@FXML
	private Button export;



	private XYChart.Series<Number,Number> series1;
	private XYChart.Series<Number,Number> series2;

	private Task<Integer> task;


	private IMAVController control;


	private MSTYPE type1_x=MSTYPE.MSP_NONE;
	private MSTYPE type1_y=MSTYPE.MSP_NONE;

	private MSTYPE type2_x=MSTYPE.MSP_NONE;
	private MSTYPE type2_y=MSTYPE.MSP_NONE;

	private BooleanProperty isCollecting = new SimpleBooleanProperty();
	private IntegerProperty timeFrame    = new SimpleIntegerProperty(30);
	private DoubleProperty scroll        = new SimpleDoubleProperty(0);

	private int resolution_ms 	= 50;


	private int current_x_pt=0;
	private int current_x0_pt=0;
	private int current_x1_pt=0;

	public XYChartWidget() {

		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("XYChartWidget.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		try {
			fxmlLoader.load();
		} catch (IOException exception) {

			throw new RuntimeException(exception);
		}

		task = new Task<Integer>() {

			@Override
			protected Integer call() throws Exception {
				while(true) {
					try {
						Thread.sleep(resolution_ms*2);
					} catch (InterruptedException iex) {
						Thread.currentThread().interrupt();
					}

					if(isDisabled()) {
						continue;
					}

					if (isCancelled()) {
						break;
					}

					if(!isCollecting.get() && control.getCollector().isCollecting()) {
						synchronized(this) {
							series1.getData().clear();
							series2.getData().clear();
						}
						current_x_pt = 0;
						scroll.setValue(0);
						updateGraph(true);
					}

					isCollecting.set(control.getCollector().isCollecting());

					if(isCollecting.get() && control.isConnected())
						updateValue(control.getCollector().getModelList().size());

				}
				return control.getCollector().getModelList().size();
			}
		};

		task.valueProperty().addListener(new ChangeListener<Integer>() {
			@Override
			public void changed(ObservableValue<? extends Integer> observableValue, Integer oldData, Integer newData) {
				updateGraph(false);

			}
		});


	}

	@FXML
	private void initialize() {


		xAxis.setAutoRanging(false);
		xAxis.setForceZeroInRange(false);
		yAxis.setAutoRanging(false);
		yAxis.setForceZeroInRange(false);

		cseries1.getItems().addAll(PRESET_NAMES);
		cseries2.getItems().addAll(PRESET_NAMES);

		linechart.setLegendVisible(false);

		cseries1_x.getItems().addAll(MSTYPE.getList());
		cseries1_y.getItems().addAll(MSTYPE.getList());
		cseries1_x.getSelectionModel().select(0);
		cseries1_y.getSelectionModel().select(0);

		cseries2_x.getItems().addAll(MSTYPE.getList());
		cseries2_y.getItems().addAll(MSTYPE.getList());
		cseries2_x.getSelectionModel().select(0);
		cseries2_y.getSelectionModel().select(0);

		scale.getItems().addAll(SCALES);

		cseries1.getSelectionModel().select(0);
		cseries2.getSelectionModel().select(0);

		scale.getSelectionModel().select(2);

		xAxis.setLowerBound(-5);
		xAxis.setUpperBound(5);
		yAxis.setLowerBound(-5);
		yAxis.setUpperBound(5);

		xAxis.setTickUnit(1); yAxis.setTickUnit(1);

		linechart.prefHeightProperty().bind(heightProperty().subtract(10));

		cseries1.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {

				cseries1_x.getSelectionModel().select(PRESETS[newValue.intValue()][0].getDescription());
				cseries1_y.getSelectionModel().select(PRESETS[newValue.intValue()][1].getDescription());

			}

		});

		cseries2.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {

				cseries2_x.getSelectionModel().select(PRESETS[newValue.intValue()][0].getDescription());
				cseries2_y.getSelectionModel().select(PRESETS[newValue.intValue()][1].getDescription());



			}

		});

		cseries1_x.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				type1_x = MSTYPE.values()[newValue.intValue()];

				String x_desc = "";
				if(type1_x!=MSTYPE.MSP_NONE)
					x_desc = x_desc + type1_x.getDescription()+" ["+type1_x.getUnit()+"]  ";


				if(type2_x!=MSTYPE.MSP_NONE)
					x_desc = x_desc + type2_x.getDescription()+" ["+type2_x.getUnit()+"]  ";

				xAxis.setLabel(x_desc);
				updateGraph(true);

			}

		});

		cseries1_y.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				type1_y = MSTYPE.values()[newValue.intValue()];

				String y_desc = "";
				if(type1_y!=MSTYPE.MSP_NONE)
					y_desc = y_desc + type1_y.getDescription()+" ["+type1_y.getUnit()+"]  ";

				if(type2_y!=MSTYPE.MSP_NONE)
					y_desc = y_desc + type2_y.getDescription()+" ["+type2_y.getUnit()+"]  ";

				yAxis.setLabel(y_desc);

				updateGraph(true);

			}

		});

		cseries2_x.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				type2_x = MSTYPE.values()[newValue.intValue()];

				String x_desc = "";
				if(type1_x!=MSTYPE.MSP_NONE)
					x_desc = x_desc + type1_x.getDescription()+" ["+type1_x.getUnit()+"]  ";

				if(type2_x!=MSTYPE.MSP_NONE)
					x_desc = x_desc + type2_x.getDescription()+" ["+type2_x.getUnit()+"]  ";

				xAxis.setLabel(x_desc);
				updateGraph(true);

			}

		});

		cseries2_y.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				type2_y = MSTYPE.values()[newValue.intValue()];

				String y_desc = "";
				if(type1_y!=MSTYPE.MSP_NONE)
					y_desc = y_desc + type1_y.getDescription()+" ["+type1_y.getUnit()+"]  ";

				if(type2_y!=MSTYPE.MSP_NONE)
					y_desc = y_desc + type2_y.getDescription()+" ["+type2_y.getUnit()+"]  ";

				yAxis.setLabel(y_desc);
				updateGraph(true);

			}

		});

		scale.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {

				if(newValue.intValue()>0) {
					float scale = Float.parseFloat(SCALES[newValue.intValue()]);

					xAxis.setAutoRanging(false);
					yAxis.setAutoRanging(false);

					xAxis.setLowerBound(-scale);
					xAxis.setUpperBound(+scale);
					yAxis.setLowerBound(-scale);
					yAxis.setUpperBound(+scale);

					if(scale>10) {
						xAxis.setTickUnit(10); yAxis.setTickUnit(10);
					} else if(scale>1) {
						xAxis.setTickUnit(1); yAxis.setTickUnit(1);
					} else {
						xAxis.setTickUnit(0.5); yAxis.setTickUnit(0.5);
					}
				} else {
					xAxis.setAutoRanging(true);
					yAxis.setAutoRanging(true);
				}

				updateGraph(true);

			}

		});


		export.setOnAction((ActionEvent event)-> {
			saveAsPng(System.getProperty("user.home"));
		});

		timeFrame.addListener((v, ov, nv) -> {

			this.current_x_pt = 0;

			if(nv.intValue() > 600) {
				resolution_ms = 500;
			}
			else if(nv.intValue() > 200) {
				resolution_ms = 200;
			}
			else if(nv.intValue() > 20) {
				resolution_ms = 100;
			}
			else
				resolution_ms = 50;


			current_x0_pt = control.getCollector().getModelList().size() - nv.intValue() * 1000 / COLLECTOR_CYCLE;
			if(current_x0_pt < 0)
				current_x0_pt = 0;

			scroll.setValue(0);
			updateGraph(true);
		});


		scroll.addListener((v, ov, nv) -> {
			if(!isCollecting.get()) {
				current_x0_pt = (int)(
						( control.getCollector().getModelList().size()  - timeFrame.get() *  1000f / COLLECTOR_CYCLE)
						* (1 - nv.intValue() / 100f))	;
				if(current_x0_pt<0)
					current_x0_pt = 0;
				if(!disabledProperty().get())
					updateGraph(true);
			}
		});


		this.disabledProperty().addListener((v, ov, nv) -> {
			if(ov.booleanValue() && !nv.booleanValue()) {
				current_x_pt = 0;
				scroll.setValue(0);
				updateGraph(true);
			}
		});



	}

	public void saveAsPng(String path) {
		WritableImage image = linechart.snapshot(new SnapshotParameters(), null);
		File file = new File(path+"/xychart.png");
		try {
			ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
		} catch (IOException e) {

		}
	}



	private void updateGraph(boolean refresh) {

		if(refresh) {
			synchronized(this) {
				series1.getData().clear();
				series2.getData().clear();
			}

			current_x_pt = current_x0_pt;
			current_x1_pt = current_x0_pt + timeFrame.intValue() * 1000 / COLLECTOR_CYCLE;

			if(current_x_pt < 0) current_x_pt = 0;
		}

		List<DataModel> mList = control.getCollector().getModelList();

		if(current_x_pt<mList.size() && mList.size()>0 ) {

			int max_x = mList.size();
			if(!isCollecting.get() && current_x1_pt < max_x)
				max_x = current_x1_pt;

			while(current_x_pt<max_x) {

				if(current_x_pt > current_x1_pt)
					current_x0_pt++;

				if(((current_x_pt * COLLECTOR_CYCLE) % resolution_ms) == 0) {

					synchronized(this) {
						if(type1_x!=MSTYPE.MSP_NONE && type1_y!=MSTYPE.MSP_NONE)
							series1.getData().add(new XYChart.Data<Number,Number>(
									MSTYPE.getValue(mList.get(current_x_pt),type1_x),
									MSTYPE.getValue(mList.get(current_x_pt),type1_y))
									);

						if(type2_x!=MSTYPE.MSP_NONE && type2_y!=MSTYPE.MSP_NONE)
							series2.getData().add(new XYChart.Data<Number,Number>(
									MSTYPE.getValue(mList.get(current_x_pt),type2_x),
									MSTYPE.getValue(mList.get(current_x_pt),type2_y))
									);

						if(current_x_pt > current_x1_pt) {
							current_x1_pt++;
							if(series1.getData().size()>0)
								series1.getData().remove(0);
							if(series2.getData().size()>0)
								series2.getData().remove(0);

						}
					}
				}

				current_x_pt++;
			}
		}
	}


	public XYChartWidget setup(IMAVController control) {
		series1 = new XYChart.Series<Number,Number>();
		linechart.getData().add(series1);
		series2 = new XYChart.Series<Number,Number>();
		linechart.getData().add(series2);

		this.control = control;

		xAxis.setLowerBound(-1);
		xAxis.setUpperBound(+1);
		yAxis.setLowerBound(-1);
		yAxis.setUpperBound(+1);

		ExecutorService.get().execute(task);
		return this;
	}


	public BooleanProperty getCollectingProperty() {
		return isCollecting;
	}

	public IntegerProperty getTimeFrameProperty() {
		return timeFrame;
	}

	@Override
	public DoubleProperty getScrollProperty() {
		return scroll;
	}


}
