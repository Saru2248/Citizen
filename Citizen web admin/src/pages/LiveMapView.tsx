import React, { useEffect, useRef, useState } from 'react';
import { MapPin, Filter, Layers, Navigation, Calendar, RefreshCw } from 'lucide-react';
import L from 'leaflet';
import { Complaint, Department, UserProfile } from '../types';

interface LiveMapViewProps {
  complaints: Complaint[];
  departments: Department[];
  workers: UserProfile[];
  onOpenComplaint: (complaint: Complaint) => void;
}

export const LiveMapView: React.FC<LiveMapViewProps> = ({
  complaints,
  departments,
  workers,
  onOpenComplaint
}) => {
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapInstanceRef = useRef<L.Map | null>(null);
  const markersLayerRef = useRef<L.LayerGroup | null>(null);

  const [filterCategory, setFilterCategory] = useState<string>('ALL');
  const [filterPriority, setFilterPriority] = useState<string>('ALL');
  const [filterStatus, setFilterStatus] = useState<string>('ALL');
  const [filterDept, setFilterDept] = useState<string>('ALL');

  // Filter complaints based on user selection
  const filteredComplaints = complaints.filter((c) => {
    if (filterCategory !== 'ALL' && c.category !== filterCategory) return false;
    if (filterPriority !== 'ALL' && c.priority !== filterPriority) return false;
    if (filterStatus !== 'ALL' && c.status !== filterStatus) return false;
    if (filterDept !== 'ALL' && c.department !== filterDept) return false;
    return true;
  });

  // Initialize Leaflet Map
  useEffect(() => {
    if (!mapContainerRef.current) return;

    if (!mapInstanceRef.current) {
      // Default center set to Pune municipal coordinates (18.5204, 73.8567) or first complaint
      const initialLat = complaints[0]?.latitude || 18.5204;
      const initialLng = complaints[0]?.longitude || 73.8567;

      const map = L.map(mapContainerRef.current, {
        center: [initialLat, initialLng],
        zoom: 12,
        zoomControl: true
      });

      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
      }).addTo(map);

      markersLayerRef.current = L.layerGroup().addTo(map);
      mapInstanceRef.current = map;
    }

    return () => {
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove();
        mapInstanceRef.current = null;
      }
    };
  }, []);

  // Update Markers on Map when filtered complaints change
  useEffect(() => {
    if (!mapInstanceRef.current || !markersLayerRef.current) return;

    markersLayerRef.current.clearLayers();

    filteredComplaints.forEach((c) => {
      if (!c.latitude || !c.longitude) return;

      // Color selection based on Priority & Status
      let color = '#3B82F6'; // Default blue
      if (c.status === 'RESOLVED') color = '#10B981'; // Green
      else if (c.priority === 'CRITICAL') color = '#EF4444'; // Red
      else if (c.priority === 'HIGH') color = '#F59E0B'; // Orange
      else if (c.status === 'IN_PROGRESS') color = '#8B5CF6'; // Purple

      // Custom Circular SVG Marker Icon
      const customIcon = L.divIcon({
        className: 'custom-leaflet-marker',
        html: `
          <div style="
            background-color: ${color};
            width: 28px;
            height: 28px;
            border-radius: 50%;
            border: 3px solid white;
            box-shadow: 0 4px 6px -1px rgba(0,0,0,0.3);
            display: flex;
            align-items: center;
            justify-content: center;
            color: white;
            font-weight: bold;
            font-size: 10px;
          ">
            ${c.complaintId.split('-')[1] || '!'}
          </div>
        `,
        iconSize: [28, 28],
        iconAnchor: [14, 14]
      });

      const marker = L.marker([c.latitude, c.longitude], { icon: customIcon });

      const popupHtml = `
        <div style="font-family: sans-serif; padding: 4px; max-width: 220px;">
          <span style="font-size: 10px; font-weight: bold; color: #64748B;">${c.complaintId}</span>
          <h4 style="margin: 2px 0 4px 0; font-size: 13px; font-weight: bold; color: #0F172A;">${c.issueType}</h4>
          <p style="font-size: 11px; color: #475569; margin-bottom: 6px;">${c.address}</p>
          <div style="margin-bottom: 6px;">
            <span style="background: ${color}; color: white; padding: 2px 6px; border-radius: 4px; font-size: 10px; font-weight: bold;">
              ${c.priority} • ${c.status}
            </span>
          </div>
          <button id="btn-${c.id}" style="
            width: 100%;
            background: #10B981;
            color: white;
            border: none;
            padding: 6px 0;
            border-radius: 6px;
            font-size: 11px;
            font-weight: bold;
            cursor: pointer;
          ">
            Open Details
          </button>
        </div>
      `;

      marker.bindPopup(popupHtml);

      marker.on('popupopen', () => {
        const btn = document.getElementById(`btn-${c.id}`);
        if (btn) {
          btn.onclick = () => onOpenComplaint(c);
        }
      });

      markersLayerRef.current?.addLayer(marker);
    });

    // Auto-fit bounds if we have valid coordinates
    if (filteredComplaints.length > 0) {
      const validPoints = filteredComplaints.filter(c => c.latitude && c.longitude);
      if (validPoints.length > 0) {
        const bounds = L.latLngBounds(validPoints.map(c => [c.latitude, c.longitude]));
        mapInstanceRef.current.fitBounds(bounds, { padding: [40, 40] });
      }
    }
  }, [filteredComplaints]);

  return (
    <div className="space-y-4 animate-in fade-in h-[calc(100vh-100px)] flex flex-col">
      {/* Live Map Control Filter Header */}
      <div className="p-4 bg-white border border-slate-200 rounded-2xl shadow-sm flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-2">
          <MapPin size={20} className="text-emerald-600" />
          <div>
            <h2 className="text-sm font-bold text-slate-900">Geospatial Live Complaint Map</h2>
            <p className="text-xs text-slate-500">
              Plotting {filteredComplaints.length} complaints on GIS map
            </p>
          </div>
        </div>

        {/* Filters */}
        <div className="flex flex-wrap items-center gap-3 text-xs">
          <select
            value={filterCategory}
            onChange={(e) => setFilterCategory(e.target.value)}
            className="px-3 py-1.5 bg-slate-50 border border-slate-200 rounded-xl font-semibold"
          >
            <option value="ALL">All Categories</option>
            <option value="POTHOLE">Pothole</option>
            <option value="GARBAGE">Garbage</option>
            <option value="STREETLIGHT">Streetlight</option>
            <option value="WATER_LEAKAGE">Water Leakage</option>
            <option value="DRAINAGE">Drainage</option>
          </select>

          <select
            value={filterPriority}
            onChange={(e) => setFilterPriority(e.target.value)}
            className="px-3 py-1.5 bg-slate-50 border border-slate-200 rounded-xl font-semibold"
          >
            <option value="ALL">All Priorities</option>
            <option value="CRITICAL">Critical</option>
            <option value="HIGH">High</option>
            <option value="NORMAL">Normal</option>
          </select>

          <select
            value={filterStatus}
            onChange={(e) => setFilterStatus(e.target.value)}
            className="px-3 py-1.5 bg-slate-50 border border-slate-200 rounded-xl font-semibold"
          >
            <option value="ALL">All Statuses</option>
            <option value="REPORTED">Reported</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="RESOLVED">Resolved</option>
          </select>

          {/* Marker Legend */}
          <div className="hidden lg:flex items-center gap-3 ml-2 pl-3 border-l border-slate-200 text-[11px]">
            <span className="flex items-center gap-1 font-semibold text-red-600">
              <span className="w-2.5 h-2.5 rounded-full bg-red-500"></span> Critical
            </span>
            <span className="flex items-center gap-1 font-semibold text-amber-600">
              <span className="w-2.5 h-2.5 rounded-full bg-amber-500"></span> High
            </span>
            <span className="flex items-center gap-1 font-semibold text-purple-600">
              <span className="w-2.5 h-2.5 rounded-full bg-purple-500"></span> In Progress
            </span>
            <span className="flex items-center gap-1 font-semibold text-emerald-600">
              <span className="w-2.5 h-2.5 rounded-full bg-emerald-500"></span> Resolved
            </span>
          </div>
        </div>
      </div>

      {/* Map Viewport Container */}
      <div className="flex-1 bg-slate-100 rounded-2xl overflow-hidden border border-slate-200 relative shadow-inner">
        <div ref={mapContainerRef} className="w-full h-full" />
      </div>
    </div>
  );
};
