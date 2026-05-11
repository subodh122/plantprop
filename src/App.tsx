import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import ReactMarkdown from 'react-markdown';
import { Leaf, ArrowLeft, RefreshCw, Smartphone, Sprout, ShieldCheck, Heart, Library, Trash2 } from 'lucide-react';
import { CameraView } from './components/CameraView';
import { analyzePlant, PlantAnalysis } from './services/botanyService';

interface SavedPlant extends PlantAnalysis {
  id: string;
  image: string;
  date: string;
}

export default function App() {
  const [view, setView] = useState<'camera' | 'results' | 'garden'>('camera');
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [result, setResult] = useState<PlantAnalysis | null>(null);
  const [capturedImage, setCapturedImage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [garden, setGarden] = useState<SavedPlant[]>([]);
  const [selectedGardenPlant, setSelectedGardenPlant] = useState<SavedPlant | null>(null);

  useEffect(() => {
    const saved = localStorage.getItem('plant_garden');
    if (saved) {
      setGarden(JSON.parse(saved));
    }
  }, []);

  const saveToGarden = () => {
    if (!result || !capturedImage) return;
    
    const newPlant: SavedPlant = {
      ...result,
      id: crypto.randomUUID(),
      image: capturedImage,
      date: new Date().toLocaleDateString(),
    };
    
    const updated = [newPlant, ...garden];
    setGarden(updated);
    localStorage.setItem('plant_garden', JSON.stringify(updated));
    setView('garden');
  };

  const deleteFromGarden = (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    const updated = garden.filter(p => p.id !== id);
    setGarden(updated);
    localStorage.setItem('plant_garden', JSON.stringify(updated));
    if (selectedGardenPlant?.id === id) setSelectedGardenPlant(null);
  };

  const handleCapture = async (base64Image: string) => {
    setIsAnalyzing(true);
    setCapturedImage(base64Image);
    setError(null);
    setView('results');
    
    try {
      const propagationInfo = await analyzePlant(base64Image);
      setResult(propagationInfo);
    } catch (err: any) {
      setError(err.message || "An unexpected error occurred.");
    } finally {
      setIsAnalyzing(false);
    }
  };

  const reset = () => {
    setResult(null);
    setCapturedImage(null);
    setIsAnalyzing(false);
    setError(null);
    setSelectedGardenPlant(null);
    setView('camera');
  };

  const openGardenPlant = (plant: SavedPlant) => {
    setSelectedGardenPlant(plant);
    setResult(plant);
    setCapturedImage(plant.image);
    setView('results');
  };

  return (
    <div className="min-h-screen flex flex-col font-sans overflow-hidden">
      {/* Background Orbs */}
      <div className="fixed inset-0 pointer-events-none z-0">
        <div className="absolute -top-[100px] -left-[100px] w-[600px] h-[600px] bg-emerald-500/15 rounded-full blur-[100px]" />
        <div className="absolute -bottom-[50px] -right-[50px] w-[500px] h-[500px] bg-teal-500/10 rounded-full blur-[100px]" />
      </div>

      {/* Header */}
      <header className="px-6 py-6 flex items-center justify-between z-20 relative">
        <div className="flex items-center gap-3 cursor-pointer" onClick={reset}>
          <div className="bg-emerald-500 p-2 rounded-xl shadow-[0_4px_12px_rgba(34,197,94,0.3)]">
            <Sprout className="text-white" size={20} />
          </div>
          <h1 className="text-xl font-bold text-white tracking-tight">PlantProp <span className="text-emerald-400 font-medium ml-1">Pro</span></h1>
        </div>
        
        <div className="flex items-center gap-3">
          <button 
            onClick={() => setView('garden')}
            className={`p-2 rounded-xl transition-all ${view === 'garden' ? 'bg-emerald-500 text-white' : 'bg-white/10 text-white/70 hover:bg-white/20'}`}
          >
            <Library size={20} />
          </button>
          {!capturedImage && view === 'camera' && (
            <div className="bg-white/10 backdrop-blur-md border border-white/10 px-3 py-1.5 rounded-lg flex items-center gap-2 text-white/50">
              <Smartphone size={14} />
              <span className="uppercase tracking-[0.15em] text-[10px] font-bold">Scanning</span>
            </div>
          )}
        </div>
      </header>

      <main className="flex-1 relative overflow-hidden px-4 pb-4">
        <AnimatePresence mode="wait">
          {view === 'camera' && (
            <motion.div
              key="camera"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="absolute inset-x-4 inset-y-0 z-0 rounded-[32px] overflow-hidden bg-black border-2 border-white/10"
            >
              <CameraView onCapture={handleCapture} isProcessing={isAnalyzing} />
            </motion.div>
          )}

          {view === 'results' && (
            <motion.div
              key="results"
              initial={{ y: '100%' }}
              animate={{ y: 0 }}
              exit={{ y: '100%' }}
              transition={{ type: 'spring', damping: 25, stiffness: 200 }}
              className="absolute inset-0 z-30 bg-transparent flex flex-col pt-2 overflow-hidden"
            >
              <div className="flex-1 glass-panel-heavy overflow-hidden flex flex-col m-2">
                <div className="px-6 py-4 flex items-center justify-between border-b border-white/10 bg-white/5">
                  <button 
                    onClick={selectedGardenPlant ? () => setView('garden') : reset}
                    className="p-2 hover:bg-white/10 rounded-xl transition-colors text-white/70"
                  >
                    <ArrowLeft size={24} />
                  </button>
                  <div className="text-center flex-1">
                    <h2 className="text-lg font-bold text-white">
                      {isAnalyzing ? 'Analyzing...' : result?.commonName || 'Botanical Analysis'}
                    </h2>
                  </div>
                  <div className="w-10" />
                </div>

                <div className="flex-1 overflow-y-auto px-6 py-6 scrollbar-hide">
                  <motion.div 
                    initial={{ opacity: 0, scale: 0.95 }}
                    animate={{ opacity: 1, scale: 1 }}
                    className="relative h-64 w-full rounded-2xl overflow-hidden mb-8 shadow-2xl border border-white/20 group"
                  >
                    <img 
                      src={capturedImage || ''} 
                      alt="Captured plant" 
                      className="w-full h-full object-cover"
                    />
                    {isAnalyzing && (
                      <div className="absolute inset-0 bg-botanic-900/60 backdrop-blur-md flex flex-col items-center justify-center text-white">
                        <RefreshCw className="animate-spin mb-3 text-emerald-400" size={32} />
                        <p className="font-medium text-sm tracking-widest uppercase opacity-80">Syncing with roots...</p>
                      </div>
                    )}
                    {result && !isAnalyzing && (
                      <div className="absolute bottom-4 left-4 bg-black/60 backdrop-blur-md border border-white/10 px-3 py-1.5 rounded-xl flex items-center gap-2">
                        <ShieldCheck size={14} className="text-emerald-400" />
                        <span className="text-white text-[10px] font-bold uppercase tracking-wider">
                          Confidence: {Math.round(result.confidence * 100)}%
                        </span>
                      </div>
                    )}
                  </motion.div>

                  {error && (
                    <div className="bg-red-500/10 border border-red-500/20 p-4 rounded-xl text-red-400 mb-6 font-medium text-center text-sm backdrop-blur-sm">
                      {error}
                    </div>
                  )}

                  {result && !isAnalyzing && (
                    <motion.div
                      initial={{ opacity: 0 }}
                      animate={{ opacity: 1 }}
                      transition={{ delay: 0.1 }}
                    >
                      <div className="mb-8">
                        <h1 className="text-3xl font-bold text-white mb-1">{result.commonName}</h1>
                        <p className="text-emerald-400 italic font-medium mb-4">{result.scientificName}</p>
                        <div className="p-4 bg-white/5 rounded-2xl border border-white/5">
                          <p className="text-white/70 text-sm leading-relaxed">{result.summary}</p>
                        </div>
                      </div>

                      <div className="markdown-body">
                        <ReactMarkdown>{`### Propagation Methods\n${result.propagationMethods}\n\n### Expert Tips\n${result.tips}`}</ReactMarkdown>
                      </div>
                    </motion.div>
                  )}
                </div>
                
                <div className="p-6 bg-white/5 border-t border-white/10 flex gap-3">
                  {!selectedGardenPlant && result && (
                    <button 
                      onClick={saveToGarden}
                      className="flex-1 py-4 px-6 bg-white/10 text-white rounded-xl font-bold border border-white/10 flex items-center justify-center gap-2 hover:bg-white/20 transition-all"
                    >
                      <Heart size={18} className="text-emerald-400" />
                      SAVE TO GARDEN
                    </button>
                  )}
                  <button 
                    onClick={reset}
                    className={`py-4 px-6 bg-emerald-500 text-white rounded-xl font-bold shadow-[0_4px_20px_rgba(34,197,94,0.3)] flex items-center justify-center gap-2 hover:bg-emerald-600 transition-all active:scale-[0.98] ${!selectedGardenPlant ? 'flex-1' : 'w-full'}`}
                  >
                    <RefreshCw size={18} />
                    {selectedGardenPlant ? 'NEW SCAN' : 'RESCAN'}
                  </button>
                </div>
              </div>
            </motion.div>
          )}

          {view === 'garden' && (
            <motion.div
              key="garden"
              initial={{ x: '100%' }}
              animate={{ x: 0 }}
              exit={{ x: '100%' }}
              className="absolute inset-0 z-30 flex flex-col pt-2 overflow-hidden"
            >
              <div className="flex-1 glass-panel-heavy overflow-hidden flex flex-col m-2">
                <div className="px-6 py-4 flex items-center justify-between border-b border-white/10 bg-white/5">
                  <button 
                    onClick={() => setView('camera')}
                    className="p-2 hover:bg-white/10 rounded-xl transition-colors text-white/70"
                  >
                    <ArrowLeft size={24} />
                  </button>
                  <div className="text-center flex-1">
                    <h2 className="text-lg font-bold text-white">My Garden</h2>
                  </div>
                  <div className="w-10" />
                </div>

                <div className="flex-1 overflow-y-auto p-4 scrollbar-hide">
                  {garden.length === 0 ? (
                    <div className="h-full flex flex-col items-center justify-center text-center p-8 opacity-40">
                      <Sprout size={64} className="mb-4" />
                      <p className="text-lg font-medium">Your garden is empty</p>
                      <p className="text-xs uppercase tracking-widest mt-2">Scan plants to add them here</p>
                    </div>
                  ) : (
                    <div className="grid grid-cols-2 gap-4">
                      {garden.map((plant) => (
                        <motion.div
                          key={plant.id}
                          layoutId={plant.id}
                          onClick={() => openGardenPlant(plant)}
                          className="relative aspect-[4/5] rounded-2xl overflow-hidden cursor-pointer group border border-white/10 shadow-lg"
                        >
                          <img src={plant.image} className="w-full h-full object-cover transition-transform group-hover:scale-105" />
                          <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-transparent to-transparent " />
                          <div className="absolute bottom-0 left-0 right-0 p-3">
                            <p className="text-white font-bold text-sm truncate">{plant.commonName}</p>
                            <p className="text-[10px] text-white/50 uppercase tracking-tighter">{plant.date}</p>
                          </div>
                          <button 
                            onClick={(e) => deleteFromGarden(plant.id, e)}
                            className="absolute top-2 right-2 p-1.5 bg-black/40 backdrop-blur-md rounded-lg text-white/70 hover:text-red-400 opacity-0 group-hover:opacity-100 transition-opacity"
                          >
                            <Trash2 size={14} />
                          </button>
                        </motion.div>
                      ))}
                    </div>
                  )}
                </div>

                <div className="p-4 bg-white/5 border-t border-white/10">
                  <button 
                    onClick={() => setView('camera')}
                    className="w-full py-4 bg-emerald-500 text-white rounded-xl font-bold flex items-center justify-center gap-2"
                  >
                    <Smartphone size={20} />
                    OPEN CAMERA
                  </button>
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </main>

      {!capturedImage && view === 'camera' && !isAnalyzing && (
        <div className="px-8 pb-10 pt-4 z-10 relative">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="glass-panel p-5 shadow-2xl flex items-center gap-4"
          >
            <div className="p-3 bg-emerald-500/20 rounded-xl text-emerald-400 border border-emerald-500/20">
              <Leaf size={24} />
            </div>
            <div>
              <p className="text-white font-bold text-sm tracking-tight mb-0.5">Ready to Analyze</p>
              <p className="text-white/40 text-[10px] leading-relaxed uppercase tracking-wider">Position plant in frame for propagation data</p>
            </div>
          </motion.div>
        </div>
      )}
    </div>
  );
}

