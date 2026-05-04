/**
 * 字典工具
 * 配合后端字典系统使用
 */
import axios from 'axios';

// 字典缓存
const dictCache = {};
const dictNameCache = {};

/**
 * 获取字典项列表
 * @param {string} dictCode - 字典类型编码
 * @returns {Promise<Array>}
 */
export async function getDict(dictCode) {
    // 先从缓存获取
    if (dictCache[dictCode]) {
        return dictCache[dictCode];
    }
    
    try {
        const response = await axios.get(`/api/sys/dict/${dictCode}`);
        if (response.data.success) {
            dictCache[dictCode] = response.data.data;
            
            // 更新名称映射缓存
            const nameMap = {};
            response.data.data.forEach(item => {
                nameMap[item.itemCode] = item.itemName;
            });
            dictNameCache[dictCode] = nameMap;
            
            return response.data.data;
        }
    } catch (error) {
        console.error(`获取字典失败: ${dictCode}`, error);
    }
    
    return [];
}

/**
 * 批量获取字典
 * @param {Array<string>} dictCodes - 字典类型编码列表
 * @returns {Promise<Object>}
 */
export async function getBatchDict(dictCodes) {
    try {
        const response = await axios.post('/api/sys/dict/batch', dictCodes);
        if (response.data.success) {
            // 更新缓存
            Object.keys(response.data.data).forEach(dictCode => {
                dictCache[dictCode] = response.data.data[dictCode];
                
                const nameMap = {};
                response.data.data[dictCode].forEach(item => {
                    nameMap[item.itemCode] = item.itemName;
                });
                dictNameCache[dictCode] = nameMap;
            });
            
            return response.data.data;
        }
    } catch (error) {
        console.error('批量获取字典失败', error);
    }
    
    return {};
}

/**
 * 获取字典项名称
 * @param {string} dictCode - 字典类型编码
 * @param {string} itemCode - 字典项编码
 * @returns {string}
 */
export function getDictName(dictCode, itemCode) {
    if (!itemCode) return '';
    
    // 先从缓存获取
    if (dictNameCache[dictCode] && dictNameCache[dictCode][itemCode]) {
        return dictNameCache[dictCode][itemCode];
    }
    
    // 缓存不存在，返回原始值
    return itemCode;
}

/**
 * 格式化字典（同步）
 * @param {string} dictCode - 字典类型编码
 * @param {string} itemCode - 字典项编码
 * @returns {string}
 */
export function formatDict(dictCode, itemCode) {
    return getDictName(dictCode, itemCode);
}

/**
 * 获取字典选项（用于下拉选择）
 * @param {string} dictCode - 字典类型编码
 * @returns {Array}
 */
export function getDictOptions(dictCode) {
    if (dictCache[dictCode]) {
        return dictCache[dictCode].map(item => ({
            label: item.itemName,
            value: item.itemCode
        }));
    }
    return [];
}

/**
 * 刷新字典缓存
 * @param {string} dictCode - 字典类型编码（可选，不传刷新所有）
 */
export async function refreshDict(dictCode) {
    if (dictCode) {
        delete dictCache[dictCode];
        delete dictNameCache[dictCode];
        await getDict(dictCode);
    } else {
        Object.keys(dictCache).forEach(code => {
            delete dictCache[code];
            delete dictNameCache[code];
        });
    }
}

/**
 * 初始化常用字典（在应用启动时调用）
 */
export async function initCommonDicts() {
    const commonDicts = [
        'knowledge_status',
        'knowledge_category',
        'audit_status',
        'user_role'
    ];
    
    await getBatchDict(commonDicts);
    console.log('[字典工具] 常用字典初始化完成');
}

// 默认导出
export default {
    getDict,
    getBatchDict,
    getDictName,
    formatDict,
    getDictOptions,
    refreshDict,
    initCommonDicts
};
