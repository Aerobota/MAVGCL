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

package com.comino.flight.widgets.charts.line;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

import javax.imageio.ImageIO;

import com.comino.flight.widgets.MovingAxis;
import com.comino.flight.widgets.SectionLineChart;
import com.comino.flight.widgets.charts.control.IChartControl;
import com.comino.mav.control.IMAVController;
import com.comino.model.types.MSTYPE;
import com.comino.msp.model.DataModel;
import com.comino.msp.model.segment.LogMessage;
import com.emxsys.chart.extension.XYAnnotations.Layer;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;


public class LineChartWidget extends BorderPane implements IChartControl {


	private static MSTYPE[][] PRESETS = {
			{ MSTYPE.MSP_NONE,		 MSTYPE.MSP_NONE,		MSTYPE.MSP_NONE		},
			{ MSTYPE.MSP_ATTROLL, 	 MSTYPE.MSP_SPATTROLL,	MSTYPE.MSP_NONE		},
			{ MSTYPE.MSP_ATTPITCH, 	 MSTYPE.MSP_SPATTPIT,	MSTYPE.MSP_NONE		},
			{ MSTYPE.MSP_ATTYAW, 	 MSTYPE.MSP_SPATTYAW,	MSTYPE.MSP_NONE		},
			{ MSTYPE.MSP_ATTROLL_R,  MSTYPE.MSP_SPATTROLL_R,MSTYPE.MSP_NONE		},
			{ MSTYPE.MSP_ATTPITCH_R, MSTYPE.MSP_SPATTPIT_R,	MSTYPE.MSP_NONE		},
			{ MSTYPE.MSP_ATTYAW_R, 	 MSTYPE.MSP_SPATTYAW_R,	MSTYPE.MSP_NONE		},
			{ MSTYPE.MSP_NEDX, 		 MSTYPE.MSP_NEDY,		MSTYPE.MSP_NEDZ		},
			{ MSTYPE.MSP_NEDX, 		 MSTYPE.MSP_SPNEDX,		MSTYPE.MSP_NONE		},
			{ MSTYPE.MSP_NEDY, 		 MSTYPE.MSP_SPNEDY,		MSTYPE.MSP_NONE		},
			{ MSTYPE.MSP_NEDZ, 		 MSTYPE.MSP_SPNEDZ,		MSTYPE.MSP_NONE 	},
			{ MSTYPE.MSP_NEDVX, 	 MSTYPE.MSP_NEDVY,		MSTYPE.MSP_NEDVZ	},
			{ MSTYPE.MSP_NEDAX, 	 MSTYPE.MSP_NEDAY,		MSTYPE.MSP_NEDAZ	 },
			{ MSTYPE.MSP_GLOBRELVX,  MSTYPE.MSP_GLOBRELVY,	MSTYPE.MSP_GLOBRELVZ },
			{ MSTYPE.MSP_DEBUGX,     MSTYPE.MSP_DEBUGY,     MSTYPE.MSP_DEBUGZ    },
			{ MSTYPE.MSP_ACCX, 		 MSTYPE.MSP_ACCY, 		MSTYPE.MSP_ACCZ 	},
			{ MSTYPE.MSP_GYROX, 	 MSTYPE.MSP_GYROY, 		MSTYPE.MSP_GYROZ 	},
			{ MSTYPE.MSP_MAGX,	     MSTYPE.MSP_MAGY, 		MSTYPE.MSP_MAGZ		},
			{ MSTYPE.MSP_RAW_FLOWX,  MSTYPE.MSP_RAW_FLOWY, 	MSTYPE.MSP_RAW_DI	},
			{ MSTYPE.MSP_VOLTAGE, 	 MSTYPE.MSP_CURRENT, 	MSTYPE.MSP_NONE		},
	};

	private static String[] PRESET_NAMES = {
			"None",
			"Att.Roll",
			"Att.Pitch",
			"Att.Yaw",
			"Att.RollRate",
			"Att.PitchRate",
			"Att.YawRate",
			"Loc.Pos.NED",
			"Loc.Pos.NED X",
			"Loc.Pos.NED Y",
			"Loc.Pos.NED Z",
			"Loc. Speed",
			"Loc. Accel.",
			"Rel.GPS.Speed",
			"Debug Values",
			"Raw Accelerator",
			"Raw Gyroskope",
			"Raw Magnetometer",
			"Raw Flow",
			"Battery",

	};

	private static int COLLECTOR_CYCLE = 50;
	private static int REFRESH_RATE    = 50;

	@FXML
	private SectionLineChart<Number, Number> linechart;

	@FXML
	private MovingAxis xAxis;

	@FXML
	private NumberAxis yAxis;

	@FXML
	private ChoiceBox<String> cseries1;

	@FXML
	private ChoiceBox<String> cseries2;

	@FXML
	private ChoiceBox<String> cseries3;

	@FXML
	private ChoiceBox<String> preset;

	@FXML
	private Button export;

	@FXML
	private CheckBox annotations;


	private  XYChart.Series<Number,Number> series1;
	private  XYChart.Series<Number,Number> series2;
	private  XYChart.Series<Number,Number> series3;

	private Task<Integer> task;

	private IMAVController control;


	private MSTYPE type1=MSTYPE.MSP_NONE;
	private MSTYPE type2=MSTYPE.MSP_NONE;
	private MSTYPE type3=MSTYPE.MSP_NONE;


	private BooleanProperty isCollecting = new SimpleBooleanProperty();
	private IntegerProperty timeFrame    = new SimpleIntegerProperty(30);
	private FloatProperty  scroll        = new SimpleFloatProperty(0);


	private int resolution_ms 	= 50;

	private int current_x_pt=0;

	private int current_x0_pt = 0;
	private int current_x1_pt = timeFrame.intValue() * 1000 / COLLECTOR_CYCLE;

	private int last_msg_pt = 0;

	private List<Data<Number,Number>> series1_list = new ArrayList<Data<Number,Number>>();
	private List<Data<Number,Number>> series2_list = new ArrayList<Data<Number,Number>>();
	private List<Data<Number,Number>> series3_list = new ArrayList<Data<Number,Number>>();

	private List<DataModel> mList = null;

	public LineChartWidget() {

		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("LineChartWidget.fxml"));
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

					try { Thread.sleep(REFRESH_RATE); } catch (InterruptedException e) { }

					if(isDisabled()) {
						try { Thread.sleep(500); } catch (InterruptedException e) { }
						continue;
					}

					if (isCancelled())
						break;

					if(!isCollecting.get() && control.getCollector().isCollecting()) {
						synchronized(this) {
							series1.getData().clear();
							series2.getData().clear();
							series3.getData().clear();
							current_x_pt = 0; current_x0_pt=0;
							setXAxisBounds(current_x0_pt,timeFrame.intValue() * 1000 / COLLECTOR_CYCLE);
						}
						scroll.setValue(0);
						Platform.runLater(() -> {
							updateGraph(false);
						});

					}

					isCollecting.set(control.getCollector().isCollecting());

					if(isCollecting.get() && control.isConnected())
						Platform.runLater(() -> {
							updateGraph(false);
						});
				}
				return 0;
			}
		};

	}

	@FXML
	private void initialize() {

		annotations.setSelected(true);

		annotations.selectedProperty().addListener((observable, oldvalue, newvalue) -> {
			Platform.runLater(() -> {
				updateGraph(true);
			});
		});

		xAxis.setAutoRanging(false);
		yAxis.setForceZeroInRange(false);
		xAxis.setLowerBound(0);
		xAxis.setLabel("Seconds");
		xAxis.setUpperBound(timeFrame.intValue());

		linechart.setLegendVisible(true);
		linechart.setLegendSide(Side.TOP);

		linechart.prefWidthProperty().bind(widthProperty());
		linechart.prefHeightProperty().bind(heightProperty());

		cseries1.getItems().addAll(MSTYPE.getList());
		cseries2.getItems().addAll(MSTYPE.getList());
		cseries3.getItems().addAll(MSTYPE.getList());

		cseries1.getSelectionModel().select(0);
		cseries2.getSelectionModel().select(0);
		cseries3.getSelectionModel().select(0);

		preset.getItems().addAll(PRESET_NAMES);
		preset.getSelectionModel().select(0);

		cseries1.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				type1 = MSTYPE.values()[newValue.intValue()];
				series1.setName(type1.getDescription()+" ["+type1.getUnit()+"]   ");
				Platform.runLater(() -> {
					updateGraph(true);
				});

			}

		});

		cseries2.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				type2 = MSTYPE.values()[newValue.intValue()];
				series2.setName(type2.getDescription()+" ["+type2.getUnit()+"]   ");
				Platform.runLater(() -> {
					updateGraph(true);
				});

			}
		});

		cseries3.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				type3 = MSTYPE.values()[newValue.intValue()];
				series3.setName(type3.getDescription()+" ["+type3.getUnit()+"]   ");
				Platform.runLater(() -> {
					updateGraph(true);
				});

			}
		});

		preset.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				type1 = PRESETS[newValue.intValue()][0];
				type2 = PRESETS[newValue.intValue()][1];
				type3 = PRESETS[newValue.intValue()][2];

				cseries1.getSelectionModel().select(type1.getDescription());
				cseries2.getSelectionModel().select(type2.getDescription());
				cseries3.getSelectionModel().select(type3.getDescription());

				series1.setName(type1.getDescription()+" ["+type1.getUnit()+"]   ");
				series2.setName(type2.getDescription()+" ["+type2.getUnit()+"]   ");
				series3.setName(type3.getDescription()+" ["+type3.getUnit()+"]   ");

				Platform.runLater(() -> {
					updateGraph(true);
				});
			}
		});

		export.setOnAction((ActionEvent event)-> {
			saveAsPng(System.getProperty("user.home"));
		});


		timeFrame.addListener((v, ov, nv) -> {
			setXResolution(nv.intValue());
		});


		scroll.addListener((v, ov, nv) -> {

			current_x0_pt = control.getCollector().calculateX0Index(nv.floatValue());;

			if(!disabledProperty().get())
				Platform.runLater(() -> {
					updateGraph(true);
				});
		});


		this.disabledProperty().addListener((v, ov, nv) -> {
			if(ov.booleanValue() && !nv.booleanValue()) {
				current_x_pt = 0;
				scroll.setValue(0);
				refreshChart();
			}
		});

		annotations.setSelected(true);
	}


	public void saveAsPng(String path) {
		SnapshotParameters param = new SnapshotParameters();
		param.setFill(Color.BLACK);
		WritableImage image = linechart.snapshot(param, null);
		File file = new File(path+"/chart.png");
		try {
			ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
		} catch (IOException e) {

		}
	}

	private void setXResolution(int frame) {
		this.current_x_pt = 0;

		if(frame > 600)
			resolution_ms = 2000;
		else if(frame > 200)
			resolution_ms = 500;
		else if(frame > 30)
			resolution_ms = 250;
		else if(frame > 20)
			resolution_ms = 100;
		else
			resolution_ms = 50;

		xAxis.setTickUnit(resolution_ms/20);
		xAxis.setMinorTickCount(10);

		scroll.setValue(0);
		xAxis.setLabel("Seconds ("+resolution_ms+"ms)");
		refreshChart();
	}



	private void updateGraph(boolean refresh) {
		float dt_sec = 0; DataModel m =null; int remove_count=0;

		series1_list.clear();
		series2_list.clear();
		series3_list.clear();

		if(refresh) {
			series1.getData().clear();
			series2.getData().clear();
			series3.getData().clear();
			linechart.getAnnotations().clearAnnotations(Layer.FOREGROUND);

			current_x_pt = current_x0_pt;
			current_x1_pt = current_x0_pt + timeFrame.intValue() * 1000 / COLLECTOR_CYCLE;
			setXAxisBounds(current_x0_pt,current_x1_pt);
		}

		if(current_x_pt > current_x1_pt) {
			current_x0_pt += REFRESH_RATE/COLLECTOR_CYCLE;
			current_x1_pt += REFRESH_RATE/COLLECTOR_CYCLE;
			setXAxisBounds(current_x0_pt,current_x1_pt);
		}

		if(current_x_pt<mList.size() && mList.size()>0 ) {

			int max_x = mList.size();
			if(!isCollecting.get() && current_x1_pt < max_x)
				max_x = current_x1_pt;

			while(current_x_pt<max_x ) {

				dt_sec = current_x_pt *  COLLECTOR_CYCLE / 1000f;

				m = mList.get(current_x_pt);

				if(m.msg!=null && current_x_pt > 0 && m.msg.msg!=null && annotations.isSelected()) {
					linechart.getAnnotations().add(new LineMessageAnnotation(dt_sec,m.msg), Layer.FOREGROUND);
				}

				if(((current_x_pt * COLLECTOR_CYCLE) % resolution_ms) == 0) {

					if(current_x_pt > current_x1_pt)
						remove_count++;

					if(type1!=MSTYPE.MSP_NONE)
						series1_list.add(new XYChart.Data<Number,Number>(dt_sec,MSTYPE.getValue(m,type1)));
					if(type2!=MSTYPE.MSP_NONE)
						series2_list.add(new XYChart.Data<Number,Number>(dt_sec,MSTYPE.getValue(m,type2)));
					if(type3!=MSTYPE.MSP_NONE)
						series3_list.add(new XYChart.Data<Number,Number>(dt_sec,MSTYPE.getValue(m,type3)));

				}
				current_x_pt++;
			}

			if(remove_count > 0) {
				if(series1.getData().size()>remove_count)
					series1.getData().remove(0, remove_count);
				if(series2.getData().size()>remove_count)
					series2.getData().remove(0, remove_count);
				if(series3.getData().size()>remove_count)
					series3.getData().remove(0, remove_count);
			}

			series1.getData().addAll(series1_list);
			series2.getData().addAll(series2_list);
			series3.getData().addAll(series3_list);

		}
	}

	private  void setXAxisBounds(float lower_pt, float upper_pt) {
		xAxis.setLowerBound(lower_pt * COLLECTOR_CYCLE / 1000F);
		xAxis.setUpperBound(upper_pt * COLLECTOR_CYCLE / 1000f);
	}


	public LineChartWidget setup(IMAVController control) {
		series1 = new XYChart.Series<Number,Number>();
		linechart.getData().add(series1);
		series2 = new XYChart.Series<Number,Number>();
		linechart.getData().add(series2);
		series3 = new XYChart.Series<Number,Number>();
		linechart.getData().add(series3);
		this.control = control;


		series1.setName(type1.getDescription());
		series2.setName(type2.getDescription());
		series3.setName(type3.getDescription());

		setXResolution(30);

		mList = control.getCollector().getModelList();

		Thread th = new Thread(task);
		th.setPriority(Thread.MIN_PRIORITY);
		th.setDaemon(true);
		th.start();

		return this;
	}


	public BooleanProperty getCollectingProperty() {
		return isCollecting;
	}


	public IntegerProperty getTimeFrameProperty() {
		return timeFrame;
	}

	@Override
	public FloatProperty getScrollProperty() {
		return scroll;
	}

	@Override
	public void refreshChart() {
		current_x0_pt = control.getCollector().calculateX0Index(1);
		if(!disabledProperty().get())
			Platform.runLater(() -> {
				updateGraph(true);
			});
	}

}
