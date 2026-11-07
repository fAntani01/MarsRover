raspivid -n -t 0 -w 480 -h 540 -fps 30 -b 3000000 -o - | gst-launch-1.0 fdsrc do-timestamp=1 ! h264parse ! rtph264pay pt=96 config-interval=5 ! udpsink clients=192.168.0.160:5001
