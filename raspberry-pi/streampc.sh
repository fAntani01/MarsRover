raspivid -n -t 0 -w 960 -h 540 -fps 30 -b 1500000 -o - | gst-launch-1.0 fdsrc do-timestamp=1 ! h264parse ! rtph264pay pt=96 config-interval=5 ! udpsink host=YOUR_PC_IP port=5001
