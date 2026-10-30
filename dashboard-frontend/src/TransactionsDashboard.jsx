import React, { useEffect, useState } from 'react';

export default function TransactionsDashboard() {
    const [transactions, setTransactions] = useState([]);
    const [loading, setLoading] = useState(true);

    // Función para consultar las transacciones desde tu API de Spring Boot
    const fetchTransactions = async () => {
        try {
            const response = await fetch('http://localhost:8080/api/iso/transactions');
            if (response.ok) {
                const data = await response.json();
                setTransactions(data);
            }
        } catch (error) {
            console.error('Error al conectar con el servidor ISO:', error);
        } finally {
            setLoading(false);
        }
    };

    // Cargar al montar el componente y actualizar cada 5 segundos (Tiempo Real)
    useEffect(() => {
        fetchTransactions();
        const interval = setInterval(fetchTransactions, 5000);
        return () => clearInterval(interval);
    }, []);

    // Cálculos para las tarjetas de resumen
    const totalAmount = transactions.reduce((acc, tx) => acc + (tx.amount || 0), 0);
    const approvedCount = transactions.filter(tx => tx.responseCode === '00').length;
    const declinedCount = transactions.length - approvedCount;

    return (
        <div style={{ padding: '30px', fontFamily: 'Arial, sans-serif', backgroundColor: '#f4f6f9', minHeight: '100vh' }}>
            <header style={{ marginBottom: '30px' }}>
                <h1 style={{ color: '#333', margin: 0 }}>💳 Panel de Control - WillStore ISO 8583</h1>
                <p style={{ color: '#666' }}>Monitoreo en tiempo real de transacciones TCP y Web</p>
            </header>

            {/* Tarjetas de Métricas */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '20px', marginBottom: '30px' }}>
                <div style={cardStyle('#3b82f6')}>
                    <h3>Total Transacciones</h3>
                    <p style={metricStyle}>{transactions.length}</p>
                </div>
                <div style={cardStyle('#10b981')}>
                    <h3>Aprobadas (00)</h3>
                    <p style={metricStyle}>{approvedCount}</p>
                </div>
                <div style={cardStyle('#ef4444')}>
                    <h3>Rechazadas / Alertas</h3>
                    <p style={metricStyle}>{declinedCount}</p>
                </div>
                <div style={cardStyle('#8b5cf6')}>
                    <h3>Monto Acumulado</h3>
                    <p style={metricStyle}>${totalAmount.toLocaleString('es-CO', { minimumFractionDigits: 2 })}</p>
                </div>
            </div>

            {/* Tabla de Transacciones */}
            <div style={{ backgroundColor: 'white', borderRadius: '8px', padding: '20px', boxShadow: '0 2px 4px rgba(0,0,0,0.1)' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '15px' }}>
                    <h2 style={{ margin: 0, fontSize: '18px', color: '#333' }}>Historial de Transacciones</h2>
                    <button onClick={fetchTransactions} style={buttonStyle}>🔄 Actualizar</button>
                </div>

                {loading ? (
                    <p>Cargando transacciones...</p>
                ) : transactions.length === 0 ? (
                    <p style={{ color: '#777' }}>No hay transacciones registradas aún. Prueba ejecutando el simulador TCP o la ruta REST.</p>
                ) : (
                    <div style={{ overflowX: 'auto' }}>
                        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
                            <thead>
                            <tr style={{ backgroundColor: '#f8fafc', borderBottom: '2px solid #e2e8f0', color: '#475569' }}>
                                <th style={thTdStyle}>ID</th>
                                <th style={thTdStyle}>Terminal (DE 41)</th>
                                <th style={thTdStyle}>PAN Enmascarado</th>
                                <th style={thTdStyle}>Monto</th>
                                <th style={thTdStyle}>Código Respuesta</th>
                            </tr>
                            </thead>
                            <tbody>
                            {transactions.slice().reverse().map((tx) => (
                                <tr key={tx.id} style={{ borderBottom: '1px solid #e2e8f0' }}>
                                    <td style={thTdStyle}>{tx.id}</td>
                                    <td style={thTdStyle}><strong>{tx.terminalId}</strong></td>
                                    <td style={thTdStyle}><code style={{ background: '#f1f5f9', padding: '2px 6px', borderRadius: '4px' }}>{tx.maskedPan}</code></td>
                                    <td style={thTdStyle}>${tx.amount?.toLocaleString('es-CO', { minimumFractionDigits: 2 })}</td>
                                    <td style={thTdStyle}>
                      <span style={{
                          padding: '4px 8px',
                          borderRadius: '4px',
                          fontSize: '12px',
                          fontWeight: 'bold',
                          backgroundColor: tx.responseCode === '00' ? '#dcfce7' : '#fee2e2',
                          color: tx.responseCode === '00' ? '#166534' : '#991b1b'
                      }}>
                        {tx.responseCode === '00' ? '00 - Aprobada' : `${tx.responseCode} - Rechazada`}
                      </span>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>
        </div>
    );
}

// Estilos auxiliares limpios
const cardStyle = (borderColor) => ({
    backgroundColor: 'white',
    padding: '20px',
    borderRadius: '8px',
    borderLeft: `5px solid ${borderColor}`,
    boxShadow: '0 2px 4px rgba(0,0,0,0.05)'
});

const metricStyle = {
    fontSize: '24px',
    fontWeight: 'bold',
    margin: '10px 0 0 0',
    color: '#1e293b'
};

const thTdStyle = {
    padding: '12px 15px',
    fontSize: '14px',
    color: '#334155'
};

const buttonStyle = {
    backgroundColor: '#2563eb',
    color: 'white',
    border: 'none',
    padding: '8px 14px',
    borderRadius: '6px',
    cursor: 'pointer',
    fontWeight: 'bold'
};