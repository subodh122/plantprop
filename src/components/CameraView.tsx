import React, { useRef, useEffect, useState } from 'react';
import { Camera, RefreshCcw, Zap } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';

interface CameraViewProps {
  onCapture: (base64Image: string) => void;
  isProcessing: boolean;
}

export const CameraView: React.FC<CameraViewProps> = ({ onCapture, isProcessing }) => {
  const videoRef = useRef<HTMLVideoElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [stream, setStream] = useState<MediaStream | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    startCamera();
    return () => {
      if (stream) {
        stream.getTracks().forEach(track => track.stop());
      }
    };
  }, []);

  const startCamera = async () => {
    try {
      const mediaStream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'environment' },
        audio: false
      });
      setStream(mediaStream);
      if (videoRef.current) {
        videoRef.current.srcObject = mediaStream;
      }
      setError(null);
    } catch (err) {
      console.error("Camera access error:", err);
      setError("Unable to access camera. Please check permissions.");
    }
  };

  const captureImage = () => {
    if (!videoRef.current || !canvasRef.current) return;

    const context = canvasRef.current.getContext('2d');
    if (!context) return;

    // Set canvas dimensions to match video
    canvasRef.current.width = videoRef.current.videoWidth;
    canvasRef.current.height = videoRef.current.videoHeight;

    // Draw the current video frame to the canvas
    context.drawImage(videoRef.current, 0, 0, canvasRef.current.width, canvasRef.current.height);

    // Convert to base64
    const imageData = canvasRef.current.toDataURL('image/jpeg', 0.85);
    onCapture(imageData);
  };

  return (
    <div className="relative w-full h-full bg-black overflow-hidden flex items-center justify-center">
      {error ? (
        <div className="flex flex-col items-center justify-center text-white p-6 text-center">
          <p className="mb-4 text-sage-300">{error}</p>
          <button 
            onClick={startCamera}
            className="flex items-center gap-2 px-6 py-2 bg-sage-600 rounded-full text-white hover:bg-sage-500 transition-colors"
          >
            <RefreshCcw size={18} /> Retry
          </button>
        </div>
      ) : (
        <>
          <video 
            ref={videoRef} 
            autoPlay 
            playsInline 
            className="w-full h-full object-cover"
          />
          <canvas ref={canvasRef} className="hidden" />
          
          <div className="absolute inset-x-0 bottom-12 flex justify-center items-center gap-8">
            <motion.button
              whileHover={{ scale: 1.1 }}
              whileTap={{ scale: 0.9 }}
              onClick={captureImage}
              disabled={isProcessing}
              className={`w-20 h-20 rounded-full border-4 border-white/30 flex items-center justify-center bg-transparent group relative ${isProcessing ? 'opacity-50 cursor-not-allowed' : ''}`}
            >
              <div className="w-16 h-16 rounded-full bg-white group-hover:bg-emerald-400 transition-all flex items-center justify-center shadow-[0_0_20px_rgba(34,197,94,0.3)]">
                {isProcessing ? (
                  <RefreshCcw className="animate-spin text-botanic-900" size={32} />
                ) : (
                  <Camera className="text-botanic-900" size={32} />
                )}
              </div>
            </motion.button>
          </div>

          <div className="absolute top-6 left-6 right-6 flex justify-between items-start pointer-events-none">
            <div className="bg-black/40 backdrop-blur-xl px-4 py-2 rounded-full border border-white/10 flex items-center gap-2">
              <Zap size={14} className="text-emerald-400 fill-emerald-400" />
              <span className="text-white text-[10px] font-bold uppercase tracking-[0.2em]">Live Analysis Mode</span>
            </div>
          </div>
        </>
      )}
    </div>
  );
};
