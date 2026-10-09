package nro.models.item;

/** Existing companion models, shared by equip validation and reconstruction. */
public final class CompanionCatalog {
    private CompanionCatalog() {}

    public static short[] parts(int itemId) {
        return switch (itemId) {
            case 2019 -> new short[]{2129, 2130, 2131};
            case 892 -> new short[]{882, 883, 884};
            case 893 -> new short[]{885, 886, 887};
            case 908 -> new short[]{891, 892, 893};
            case 909 -> new short[]{894, 895, 896};
            case 910 -> new short[]{897, 898, 899};
            case 916 -> new short[]{925, 926, 927};
            case 917 -> new short[]{928, 929, 930};
            case 918 -> new short[]{931, 932, 933};
            case 919 -> new short[]{934, 935, 936};
            case 936 -> new short[]{718, 719, 720};
            case 942 -> new short[]{966, 967, 968};
            case 943 -> new short[]{969, 970, 971};
            case 944 -> new short[]{972, 973, 974};
            case 967 -> new short[]{1050, 1051, 1052};
            case 1008 -> new short[]{1074, 1075, 1076};
            case 1039 -> new short[]{1089, 1090, 1091};
            case 1040 -> new short[]{1092, 1093, 1094};
            case 1046 -> new short[]{-1, -1, -1};
            case 1107 -> new short[]{1155, 1156, 1157};
            case 1114 -> new short[]{1158, 1159, 1160};
            case 1188 -> new short[]{1183, 1184, 1185};
            case 1202 -> new short[]{1201, 1202, 1203};
            case 1203 -> new short[]{1201, 1202, 1203};
            case 1207 -> new short[]{1077, 1078, 1079};
            case 1224 -> new short[]{1227, 1228, 1229};
            case 1225 -> new short[]{1233, 1234, 1235};
            case 1226 -> new short[]{1230, 1231, 1232};
            case 1243 -> new short[]{1245, 1246, 1247};
            case 1244 -> new short[]{1248, 1249, 1250};
            case 1256 -> new short[]{1267, 1268, 1269};
            case 1318 -> new short[]{1299, 1300, 1301};
            case 1347 -> new short[]{1302, 1303, 1304};
            case 1414 -> new short[]{1341, 1342, 1343};
            case 1435 -> new short[]{1347, 1348, 1349};
            case 1452 -> new short[]{1365, 1366, 1367};
            case 1458 -> new short[]{1368, 1369, 1370};
            case 1482 -> new short[]{1398, 1399, 1400};
            case 1497 -> new short[]{1401, 1402, 1403};
            case 1550 -> new short[]{1428, 1429, 1430};
            case 1551 -> new short[]{1425, 1426, 1427};
            case 1564 -> new short[]{1437, 1438, 1439};
            case 1568 -> new short[]{1443, 1444, 1445};
            case 1573 -> new short[]{1446, 1447, 1448};
            case 1596 -> new short[]{1473, 1474, 1475};
            case 1597 -> new short[]{1473, 1474, 1475};
            case 1611 -> new short[]{1488, 1494, 1495};
            case 1620 -> new short[]{1496, 1497, 1498};
            case 1621 -> new short[]{1496, 1497, 1498};
            case 1622 -> new short[]{1488, 1489, 1490};
            case 1629 -> new short[]{1505, 1506, 1507};
            case 1630 -> new short[]{1508, 1509, 1510};
            case 1631 -> new short[]{1513, 1516, 1517};
            case 1633 -> new short[]{1523, 1524, 1525};
            case 1654 -> new short[]{1526, 1529, 1530};
            case 1668 -> new short[]{1550, 1551, 1552};
            case 1682 -> new short[]{1558, 1559, 1560};
            case 1683 -> new short[]{1561, 1562, 1563};
            case 1686 -> new short[]{1572, 1573, 1574};
            case 1750 -> new short[]{1464, 1465, 1466};
            case 1765 -> new short[]{1662, 1663, 1664};
            case 1729 -> new short[]{1621, 1622, 1623};
            case 1727 -> new short[]{1616, 1617, 1618};
            case 1789 -> new short[]{1724, 1725, 1726};
            case 1766 -> new short[]{1665, 1666, 1667};
            case 1767 -> new short[]{1668, 1669, 1670};
            case 1768 -> new short[]{1671, 1672, 1673};
            case 1769 -> new short[]{1674, 1675, 1676};
            case 1770 -> new short[]{1677, 1678, 1679};
            case 1771 -> new short[]{1680, 1681, 1682};
            default -> null;
        };
    }
}
